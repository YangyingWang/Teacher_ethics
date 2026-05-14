# -*- coding: utf-8 -*-
"""
批量处理课程视频：
1. 读取 manifest.csv
2. 提取视频时长
3. 截封面
4. 截取前 3 分钟音频并用 openai-whisper 转写
5. 调用 DeepSeek 生成 title / description / difficulty / teacher_type / keywords
6. 动态计算 rating_avg / rating_count / hot_score
7. 将视频复制到 D:/Major/course-import/videos/
8. 将封面保存到 D:/Major/course-import/covers/
9. 导出 CSV：完整结果 + 可导入数据库结果
"""

import os
import re
import json
import math
import time
import shutil
import hashlib
import subprocess
from pathlib import Path
from typing import Dict, List, Optional

import pandas as pd
import requests
from tqdm import tqdm
import whisper


# ========= 1. 基础配置 =========
BASE_DIR = Path(r"D:\Major\course-import")
SOURCE_VIDEO_ROOT = BASE_DIR / "videos"
MANIFEST_FILE = BASE_DIR / "manifest.csv"
OUTPUT_DIR = BASE_DIR / "output"
TEMP_AUDIO_DIR = OUTPUT_DIR / "temp_audio"

# Spring Boot 静态资源根目录
STATIC_VIDEO_DIR = BASE_DIR / "videos"
STATIC_COVER_DIR = BASE_DIR / "covers"

# 你的后端 static-locations 指向 D:/Major/course-import/
# 所以数据库里 URL 应该写成下面这种
VIDEO_URL_PREFIX = "/videos/"
COVER_URL_PREFIX = "/covers/"

# 如果 conda 环境里 ffmpeg 仍然找不到，就填 bin 目录；否则留空
FFMPEG_BIN = r"D:\ffmpeg-8.0.1-essentials_build\bin"

# Whisper 配置（CPU 建议 tiny 或 base）
WHISPER_MODEL_NAME = "base"
TRANSCRIBE_SECONDS = 300

# DeepSeek 配置
DEEPSEEK_API_KEY = "sk-xxx"
DEEPSEEK_API_URL = "https://api.deepseek.com/chat/completions"
DEEPSEEK_MODEL = "deepseek-chat"
DEEPSEEK_TIMEOUT = 120
DEEPSEEK_RETRY = 2

# 课程默认状态
DEFAULT_STATUS = 1

# 枚举映射
# difficulty：0-初级 1-中级 2-高级
DIFFICULTY_MAP = {
    "初级": 1,
    "中级": 2,
    "高级": 3
}
# teacher_type 映射：与 users.type 保持一致
TEACHER_TYPE_MAP = {
    "教学型": 0,
    "科研型": 1,
    "综合型": 2
}
ALLOWED_DIFFICULTY_NAMES = set(DIFFICULTY_MAP.keys())
ALLOWED_TEACHER_TYPE_NAMES = set(TEACHER_TYPE_MAP.keys())

# 是否保留临时音频
KEEP_TEMP_AUDIO = False

# ========= 2. 初始化目录和 PATH =========
if FFMPEG_BIN.strip():
    os.environ["PATH"] = FFMPEG_BIN + ";" + os.environ["PATH"]

OUTPUT_DIR.mkdir(parents=True, exist_ok=True)
TEMP_AUDIO_DIR.mkdir(parents=True, exist_ok=True)
STATIC_VIDEO_DIR.mkdir(parents=True, exist_ok=True)
STATIC_COVER_DIR.mkdir(parents=True, exist_ok=True)


# ========= 3. 工具函数 =========
def run_cmd(cmd: List[str]) -> str:
    result = subprocess.run(
        cmd,
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
        text=True,
        encoding="utf-8",
        errors="ignore"
    )
    if result.returncode != 0:
        raise RuntimeError(f"命令执行失败: {' '.join(cmd)}\n{result.stderr}")
    return result.stdout.strip()


def safe_name(name: str) -> str:
    name = re.sub(r'[\\/:*?"<>|]+', "_", name)
    name = re.sub(r"\s+", "_", name)
    return name.strip("_")


def find_video_file(filename: str) -> Optional[Path]:
    matches = list(SOURCE_VIDEO_ROOT.rglob(filename))
    return matches[0] if matches else None


def get_video_duration_seconds(video_path: Path) -> int:
    cmd = [
        "ffprobe",
        "-v", "error",
        "-show_entries", "format=duration",
        "-of", "default=noprint_wrappers=1:nokey=1",
        str(video_path)
    ]
    out = run_cmd(cmd)
    return max(1, int(float(out)))


def get_video_duration_minutes(video_path: Path) -> int:
    sec = get_video_duration_seconds(video_path)
    return max(1, math.ceil(sec / 60))


def build_public_basename(video_path: Path) -> str:
    rel = str(video_path.relative_to(SOURCE_VIDEO_ROOT)).replace("\\", "/")
    digest = hashlib.md5(rel.encode("utf-8")).hexdigest()[:8]
    return f"{safe_name(video_path.stem)}_{digest}"


def copy_video_to_static(video_path: Path, public_video_name: str) -> Path:
    target = STATIC_VIDEO_DIR / public_video_name
    if not target.exists():
        shutil.copy2(video_path, target)
    return target


def extract_cover(video_path: Path, cover_path: Path, at_second: int):
    cmd = [
        "ffmpeg",
        "-y",
        "-ss", str(at_second),
        "-i", str(video_path),
        "-frames:v", "1",
        "-q:v", "2",
        str(cover_path)
    ]
    run_cmd(cmd)


def extract_audio_clip(video_path: Path, audio_path: Path, seconds: int = 180):
    cmd = [
        "ffmpeg",
        "-y",
        "-i", str(video_path),
        "-t", str(seconds),
        "-vn",
        "-acodec", "pcm_s16le",
        "-ar", "16000",
        "-ac", "1",
        str(audio_path)
    ]
    run_cmd(cmd)


def transcribe_audio(model, audio_path: Path) -> str:
    result = model.transcribe(
        str(audio_path),
        language="zh",
        fp16=False,
        verbose=False
    )
    text = result.get("text", "") or ""
    text = re.sub(r"\s+", " ", text).strip()
    return text


def infer_teacher_type_name_fallback(category_name: str, transcript: str) -> str:
    text = f"{category_name} {transcript}"

    research_keywords = ["科研", "学术", "论文", "项目", "课题", "数据", "署名", "基金", "研究", "成果发表"]
    teaching_keywords = ["教学", "课堂", "课程", "学生", "授课", "教学设计", "课堂管理", "育人", "作业", "考试"]

    research_hit = sum(1 for k in research_keywords if k in text)
    teaching_hit = sum(1 for k in teaching_keywords if k in text)

    if research_hit >= teaching_hit + 2:
        return "科研型"
    if teaching_hit >= research_hit + 2:
        return "教学型"
    return "综合型"

def infer_difficulty_name_fallback(duration_min: int, transcript: str) -> str:
    text_len = len(transcript or "")
    if duration_min >= 60 or text_len >= 1500:
        return "高级"
    if duration_min >= 30 or text_len >= 700:
        return "中级"
    return "初级"


def fallback_generate(video_stem: str, transcript: str, category_name: str, duration_min: int) -> Dict:
    teacher_type_name = infer_teacher_type_name_fallback(category_name, transcript)
    difficulty_name = infer_difficulty_name_fallback(duration_min, transcript)

    title = video_stem.strip()
    if not title:
        title = f"{category_name}专题课程"
    title = re.sub(r"[_\-]+", " ", title).strip()
    if len(title) > 25:
        title = title[:25]

    if transcript:
        description = (
            f"课程围绕{category_name}相关内容展开，结合高校教师职业规范与教育实践场景，"
            f"帮助教师理解核心要求、识别潜在风险并提升规范履职与价值引领能力。"
        )
    else:
        description = (
            f"课程围绕{category_name}主题展开，面向高校教师梳理相关规范要求、典型问题与实践要点，"
            f"用于支持师德师风教育学习与职业行为规范提升。"
        )
    keywords = [category_name, "高校教师", "师德师风"]

    return {
        "title": title,
        "description": description,
        "difficultyName": difficulty_name,
        "teacherTypeName": teacher_type_name,
        "keywords": keywords
    }


def normalize_llm_result(data: Dict, video_stem: str, category_name: str, transcript: str, duration_min: int) -> Dict:
    fallback = fallback_generate(video_stem, transcript, category_name, duration_min)

    title = str(data.get("title", "")).strip() or fallback["title"]
    if len(title) > 25:
        title = title[:25]

    description = str(data.get("description", "")).strip() or fallback["description"]

    difficulty_name = str(data.get("difficultyName", "")).strip()
    if difficulty_name not in ALLOWED_DIFFICULTY_NAMES:
        difficulty_name = fallback["difficultyName"]

    teacher_type_name = str(data.get("teacherTypeName", "")).strip()
    if teacher_type_name not in ALLOWED_TEACHER_TYPE_NAMES:
        teacher_type_name = fallback["teacherTypeName"]

    keywords = data.get("keywords", [])
    if isinstance(keywords, str):
        keywords = [x.strip() for x in re.split(r"[，,、;；\s]+", keywords) if x.strip()]
    elif isinstance(keywords, list):
        keywords = [str(x).strip() for x in keywords if str(x).strip()]
    else:
        keywords = fallback["keywords"]

    if not keywords:
        keywords = fallback["keywords"]

    keywords = keywords[:5]

    return {
        "title": title,
        "description": description,
        "difficultyName": difficulty_name,
        "teacherTypeName": teacher_type_name,
        "keywords": keywords
    }

def call_deepseek_generate(video_stem: str, transcript: str, category_name: str, duration_min: int) -> Dict:
    if not DEEPSEEK_API_KEY or "请替换" in DEEPSEEK_API_KEY:
        return fallback_generate(video_stem, transcript, category_name, duration_min)

    prompt = f"""
请根据给定信息，生成课程元数据，并且只输出 JSON。

要求：
1. category 已知，不需要判断 category
2. 输出必须是合法 JSON
3. title 正式简洁，不超过 25 个字
4. description 控制在 80-150 字
5. difficultyName 只能是：初级 / 中级 / 高级
6. teacherTypeName 只能是：教学型 / 科研型 / 综合型
7. keywords 返回 3-5 个
8. 内容要贴合高校教师师德师风教育场景

输入信息：
- 已知类别：{category_name}
- 文件名参考：{video_stem}
- 时长（分钟）：{duration_min}
- 转写文本：{transcript[:3000]}

JSON 输出示例：
{{
  "title": "高校教师科研诚信导论",
  "description": "课程围绕高校教师科研活动中的学术规范、数据真实性、署名伦理与成果发表要求展开，帮助教师识别科研失范风险，提升科研诚信意识与规范实践能力。",
  "difficultyName": "初级",
  "teacherTypeName": "科研型",
  "keywords": ["科研诚信", "学术规范", "高校教师"]
}}
""".strip()

    payload = {
        "model": DEEPSEEK_MODEL,
        "messages": [
            {
                "role": "system",
                "content": "你是一个高校师德师风课程整理助手，必须只输出 JSON。"
            },
            {
                "role": "user",
                "content": prompt
            }
        ],
        "response_format": {"type": "json_object"},
        "temperature": 0.2,
        "max_tokens": 800,
        "stream": False
    }

    headers = {
        "Content-Type": "application/json",
        "Authorization": f"Bearer {DEEPSEEK_API_KEY}"
    }

    last_error = None
    for _ in range(DEEPSEEK_RETRY + 1):
        try:
            resp = requests.post(
                DEEPSEEK_API_URL,
                headers=headers,
                json=payload,
                timeout=DEEPSEEK_TIMEOUT
            )
            resp.raise_for_status()
            data = resp.json()
            content = data["choices"][0]["message"]["content"]

            if not content or not str(content).strip():
                raise RuntimeError("DeepSeek 返回空内容")

            parsed = json.loads(content)
            return normalize_llm_result(parsed, video_stem, category_name, transcript, duration_min)
        except Exception as e:
            last_error = e
            time.sleep(1)

    print(f"[警告] DeepSeek 生成失败，已改用兜底规则。错误：{last_error}")
    return fallback_generate(video_stem, transcript, category_name, duration_min)


def calc_dynamic_metrics(duration_min: int, transcript: str, difficulty: int, teacher_type: int, keywords: List[str]) -> Dict:
    """
    这些是课程初始化时的动态演示值，不是用户真实行为统计。
    用来让不同课程导入时，enrollmentCount / hotScore / isFeatured 不是固定常量。
    """
    text_len = len(transcript or "")
    kw_count = len(keywords or [])

    content_factor = min(text_len / 1800, 1.0)
    duration_factor = min(duration_min / 60, 1.0)
    difficulty_factor = difficulty * 0.12
    type_factor = 0.08 if teacher_type == 2 else (0.06 if teacher_type == 1 else 0.04)

    enrollment_count = int(30 + duration_min * 2.2 + text_len / 18 + kw_count * 10)
    enrollment_count = max(30, min(enrollment_count, 500))

    hot_score = 55 + content_factor * 18 + duration_factor * 12 + difficulty_factor * 10 + type_factor * 10 + enrollment_count * 0.15
    hot_score = round(min(hot_score, 100.0), 2)

    is_featured = 1 if hot_score >= 82 or (difficulty == 2 and enrollment_count >= 120) else 0

    return {
        "enrollmentCount": enrollment_count,
        "hotScore": hot_score,
        "isFeatured": is_featured
    }

def build_video_url(public_video_name: str) -> str:
    return VIDEO_URL_PREFIX + public_video_name


def build_cover_url(public_cover_name: str) -> str:
    return COVER_URL_PREFIX + public_cover_name


# ========= 4. 主流程 =========
def main():
    if not MANIFEST_FILE.exists():
        raise FileNotFoundError(f"未找到 manifest.csv: {MANIFEST_FILE}")

    if not SOURCE_VIDEO_ROOT.exists():
        raise FileNotFoundError(f"未找到视频目录: {SOURCE_VIDEO_ROOT}")

    df = pd.read_csv(MANIFEST_FILE)
    required_cols = {"filename", "category_id", "category_name"}
    if not required_cols.issubset(set(df.columns)):
        raise ValueError("manifest.csv 必须包含 filename, category_id, category_name 三列")

    print(f"加载 Whisper 模型：{WHISPER_MODEL_NAME}")
    model = whisper.load_model(WHISPER_MODEL_NAME)

    results = []
    failed = []

    for _, row in tqdm(df.iterrows(), total=len(df), desc="处理进度"):
        filename = str(row["filename"]).strip()
        category_id = int(row["category_id"])
        category_name = str(row["category_name"]).strip()

        video_path = find_video_file(filename)
        if video_path is None:
            failed.append({"filename": filename, "reason": "未找到视频文件"})
            continue

        try:
            duration_sec = get_video_duration_seconds(video_path)
            duration_min = max(1, math.ceil(duration_sec / 60))

            public_base = build_public_basename(video_path)
            public_video_name = public_base + video_path.suffix.lower()
            public_cover_name = public_base + ".jpg"

            # 1. 复制视频到 Spring Boot 静态资源目录
            copy_video_to_static(video_path, public_video_name)

            # 2. 生成封面到静态资源目录
            cover_second = min(10, max(2, duration_sec // 3))
            cover_path = STATIC_COVER_DIR / public_cover_name
            if not cover_path.exists():
                extract_cover(video_path, cover_path, at_second=cover_second)

            # 3. 提取音频并转写
            audio_path = TEMP_AUDIO_DIR / f"{public_base}.wav"
            extract_audio_clip(video_path, audio_path, seconds=TRANSCRIBE_SECONDS)
            transcript = transcribe_audio(model, audio_path)

            # 4. DeepSeek 生成课程元数据
            llm_result = call_deepseek_generate(
                video_stem=video_path.stem,
                transcript=transcript,
                category_name=category_name,
                duration_min=duration_min
            )

            difficulty = DIFFICULTY_MAP[llm_result["difficultyName"]]
            teacher_type = TEACHER_TYPE_MAP[llm_result["teacherTypeName"]]
            keywords = llm_result["keywords"]

            # 5. 动态生成热度相关字段
            metrics = calc_dynamic_metrics(
                duration_min=duration_min,
                transcript=transcript,
                difficulty=difficulty,
                teacher_type=teacher_type,
                keywords=keywords
            )

            record = {
                "categoryId": category_id,
                "title": llm_result["title"],
                "description": llm_result["description"],
                "coverUrl": build_cover_url(public_cover_name),
                "videoUrl": build_video_url(public_video_name),
                "difficulty": difficulty,
                "duration": duration_min,
                "teacherType": teacher_type,
                "enrollmentCount": metrics["enrollmentCount"],
                "hotScore": metrics["hotScore"],
                "isFeatured": metrics["isFeatured"],
                "status": DEFAULT_STATUS,
                "categoryName": category_name,
                "teacherTypeName": llm_result["teacherTypeName"],
                "difficultyName": llm_result["difficultyName"],
                "keywords": ",".join(keywords),
                "transcript": transcript,
                "sourceFilename": filename
            }

            results.append(record)

        except Exception as e:
            failed.append({
                "filename": filename,
                "reason": str(e)
            })
        finally:
            if audio_path and audio_path.exists() and not KEEP_TEMP_AUDIO:
                try:
                    audio_path.unlink()
                except Exception:
                    pass

    if not results:
        print("没有成功处理的数据。")
        if failed:
            pd.DataFrame(failed).to_csv(
                OUTPUT_DIR / "failed_rows.csv",
                index=False,
                encoding="utf-8-sig"
            )
        return

    full_df = pd.DataFrame(results)

    # 完整中间结果
    full_df.to_csv(
        OUTPUT_DIR / "courses_import_full.csv",
        index=False,
        encoding="utf-8-sig"
    )

    # 仅导出适配 Course 实体的字段
    db_fields = [
        "categoryId",
        "title",
        "description",
        "coverUrl",
        "videoUrl",
        "difficulty",
        "duration",
        "teacherType",
        "enrollmentCount",
        "hotScore",
        "isFeatured",
        "status"
    ]
    db_df = full_df[db_fields].copy()
    db_df.to_csv(
        OUTPUT_DIR / "courses_db_import.csv",
        index=False,
        encoding="utf-8-sig"
    )

    if failed:
        pd.DataFrame(failed).to_csv(
            OUTPUT_DIR / "failed_rows.csv",
            index=False,
            encoding="utf-8-sig"
        )

    print("\n处理完成")
    print(f"完整结果: {OUTPUT_DIR / 'courses_import_full.csv'}")
    print(f"数据库导入结果: {OUTPUT_DIR / 'courses_db_import.csv'}")
    if failed:
        print(f"失败记录: {OUTPUT_DIR / 'failed_rows.csv'}")


if __name__ == "__main__":
    main()