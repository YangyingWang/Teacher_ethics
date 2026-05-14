# -*- coding: utf-8 -*-
"""
将 courses_db_import.csv 转为 MySQL 可执行的 INSERT INTO SQL 文件
适配字段：
categoryId,title,description,coverUrl,videoUrl,difficulty,duration,
teacherType,enrollmentCount,hotScore,isFeatured,status

说明：
1. 不包含 id、createdAt、updatedAt，这些由数据库自动处理
2. 默认表名为 courses
3. 默认输入文件名为 courses_db_import.csv
4. 默认输出文件名为 courses_import.sql
"""

from __future__ import annotations

import math
from decimal import Decimal, InvalidOperation
from pathlib import Path
import pandas as pd


INPUT_CSV = Path("D:\Major\course-import\output\courses_db_import.csv")
OUTPUT_SQL = Path("courses_import.sql")
TABLE_NAME = "courses"
BATCH_SIZE = 20  # 每批多少条 INSERT，可按需调整

COLUMNS = [
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
    "status",
]

DB_COLUMNS = [
    "category_id",
    "title",
    "description",
    "cover_url",
    "video_url",
    "difficulty",
    "duration",
    "teacher_type",
    "enrollment_count",
    "hot_score",
    "is_featured",
    "status",
]


def is_null_like(value) -> bool:
    if value is None:
        return True
    try:
        if pd.isna(value):
            return True
    except Exception:
        pass
    if isinstance(value, str) and value.strip() == "":
        return True
    return False


def sql_escape_string(value: str) -> str:
    """转义 MySQL 字符串"""
    value = value.replace("\\", "\\\\")
    value = value.replace("'", "\\'")
    value = value.replace("\r", "\\r")
    value = value.replace("\n", "\\n")
    value = value.replace("\t", "\\t")
    value = value.replace("\x00", "")
    return value


def to_sql_literal(value, col: str) -> str:
    if is_null_like(value):
        return "NULL"

    if col in {"title", "description", "coverUrl", "videoUrl"}:
        return f"'{sql_escape_string(str(value))}'"

    if col == "hotScore":
        try:
            dec = Decimal(str(value))
            return format(dec, 'f')
        except (InvalidOperation, ValueError):
            return "0"

    # 其余整数列
    try:
        if isinstance(value, float):
            if math.isnan(value):
                return "NULL"
            return str(int(round(value)))
        return str(int(value))
    except Exception:
        return "0"



def validate_dataframe(df: pd.DataFrame):
    missing = [c for c in COLUMNS if c not in df.columns]
    if missing:
        raise ValueError(f"CSV 缺少字段: {missing}")



def build_insert_sql(df: pd.DataFrame) -> str:
    lines = []
    lines.append("-- 由 csv_to_courses_sql.py 自动生成")
    lines.append(f"-- 源文件: {INPUT_CSV.name}")
    lines.append("SET NAMES utf8mb4;")
    lines.append("SET FOREIGN_KEY_CHECKS = 0;")
    lines.append("")

    total = len(df)
    for start in range(0, total, BATCH_SIZE):
        chunk = df.iloc[start:start + BATCH_SIZE]
        lines.append(
            f"INSERT INTO `{TABLE_NAME}` ({', '.join(f'`{c}`' for c in DB_COLUMNS)}) VALUES"
        )

        value_lines = []
        for _, row in chunk.iterrows():
            vals = [to_sql_literal(row[col], col) for col in COLUMNS]
            value_lines.append("  (" + ", ".join(vals) + ")")

        lines.append(",\n".join(value_lines) + ";")
        lines.append("")

    lines.append("SET FOREIGN_KEY_CHECKS = 1;")
    lines.append("")
    return "\n".join(lines)



def main():
    if not INPUT_CSV.exists():
        raise FileNotFoundError(f"未找到输入文件: {INPUT_CSV.resolve()}")

    df = pd.read_csv(INPUT_CSV)
    validate_dataframe(df)
    df = df[COLUMNS].copy()

    sql_text = build_insert_sql(df)
    OUTPUT_SQL.write_text(sql_text, encoding="utf-8")

    print(f"已读取 {len(df)} 条课程记录")
    print(f"已生成 SQL 文件: {OUTPUT_SQL.resolve()}")


if __name__ == "__main__":
    main()
