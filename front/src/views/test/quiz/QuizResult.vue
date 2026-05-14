<script setup>
import { ArrowLeft, ArrowRight} from '@element-plus/icons-vue'
import {ElMessage, ElMessageBox, ElLoading } from 'element-plus'
import { QuillEditor } from '@vueup/vue-quill'
import '@vueup/vue-quill/dist/vue-quill.snow.css'

import { ref, computed, onMounted} from 'vue'
import { getResultService, createFullReportService } from '@/api/test.js'
import { useRouter} from 'vue-router'
const router = useRouter()
const qnId = router.currentRoute.value.query.id

const resultDTO = ref(null)
const title = ref('答卷详情')
const currentType=ref(0);
const topicCount = ref([0, 0, 0])
const totalQuestions = ref(0)
const topic = ref({ 
    0: [], // 判断题
    1: [], // 选择题
    2: []  // 简答题
})

const index = ref(0)
const number = ref(1)
const showQuestion = ref({})

// ===== 工具：格式化 =====
const formatSpent = (sec) => {
  if (sec === null || sec === undefined) return '-'
  const h = String(Math.floor(sec / 3600)).padStart(2, '0')
  const m = String(Math.floor((sec % 3600) / 60)).padStart(2, '0')
  const s = String(sec % 60).padStart(2, '0')
  return `${h}:${m}:${s}`
}
// choice：A->1
const letterToNum = (ch) => {
  if (!ch) return null
  const s = String(ch).trim().toUpperCase()
  return s.charCodeAt(0) - 64 // A=1
}
const numToLetter = (n) => String.fromCharCode(64 + Number(n)) // 1->A
const stripHtml = (html) => {
  if (html === null || html === undefined) return ''
  return String(html)
    .replace(/<br\s*\/?>/gi, '')
    .replace(/<\/?p[^>]*>/gi, '')
    .replace(/<\/?div[^>]*>/gi, '')
    .replace(/&nbsp;/gi, ' ')
    .replace(/<[^>]+>/g, '')
    .trim()
}

// 将后端题目数据转换为前端格式
const transformResult = (questionResults) => {
    let judgeIndex = 0;
    let changeIndex = 0;
    let fillIndex = 0;
    
    questionResults.forEach(q => {
        const base = {
            id: q.id,
            type: q.type,
            content: q.content,
            difficulty: q.difficulty,
            score: q.score,
            analysis: q.analysis,

            userAnswer: q.userAnswer,
            userScore: q.userScore,
            correctAnswer: q.correctAnswer,
            isMark: !!q.isMarked,
            isClick: q.userAnswer !== null && q.userAnswer !== undefined && stripHtml(q.userAnswer).length !== 0
        }
        if (q.type === 0) { // 判断题
            topic.value[0].push({...base, index: judgeIndex++ })
        } else if (q.type === 1) { // 选择题
            topic.value[1].push({
                ...base,
                optionA: q.optionA,
                optionB: q.optionB,
                optionC: q.optionC,
                optionD: q.optionD,
                isMultiple: q.isMultiple,
                index: changeIndex++
            })
        } else if (q.type === 2) { // 简答题
            topic.value[2].push({
                ...base,
                reference: q.reference,
                keyword: q.keyword,
                index: fillIndex++
            })
        }
    })
    topicCount.value[0] = topic.value[0].length
    topicCount.value[1] = topic.value[1].length
    topicCount.value[2] = topic.value[2].length
    totalQuestions.value = topicCount.value[0] + topicCount.value[1] + topicCount.value[2]
}

// ===== 切题 =====
const judge = (idx) => {
  currentType.value = 0;
  index.value = idx;
  if(idx < topic.value[0].length){
    if(idx <= 0){
        index.value = 0;
    }
    title.value = '判断题';
    showQuestion.value = topic.value[0][index.value];
    number.value = index.value + 1;
  }else{
    change(0);
  }
}
const change = (idx) => {
  currentType.value = 1;
  index.value = idx;
  if(idx < topic.value[1].length){
    if(idx < 0){
        judge(topic.value[0].length -1);
    }else{
        title.value = '选择题';
        showQuestion.value = topic.value[1][idx];
        number.value = topicCount.value[0] + idx + 1;
    }
  }else{
    fill(0);
  }
}
const fill = (idx) => {
  currentType.value = 2;
  index.value = idx;
  if(idx < topic.value[2].length){
    if(idx < 0){
        change(topic.value[1].length -1);
    }else{
        title.value = '简答题';
        showQuestion.value = topic.value[2][idx];
        number.value = topicCount.value[0] + topicCount.value[1] + idx + 1;
    }
  }else{
    index.value = topic.value[2].length - 1;
    // judge(0);
  }
}
const previous = () => {
  index.value--;
  switch (currentType.value) {
    case 0: judge(index.value); break;
    case 1: change(index.value); break;
    case 2: fill(index.value); break;
  }
}
const next = () => {
  index.value++;
  switch (currentType.value) {
    case 0: judge(index.value); break;
    case 1: change(index.value); break;
    case 2: fill(index.value); break;
  }
}
// ===== 只读回显：把后端 userAnswer/correctAnswer 转成控件需要的值 =====
const judgeModelValue = computed(() => {
  if (currentType.value !== 0) return null
  const ua = showQuestion.value.userAnswer
  if (ua === true || ua === 1) return 1
  if (ua === false || ua === 2) return 2
  return null
})

const choiceModelValue = computed(() => {
  if (currentType.value !== 1) return null
  const ua = showQuestion.value.userAnswer
  if (showQuestion.value.isMultiple) {
    if (Array.isArray(ua)) return ua.map(letterToNum).filter(Boolean)
    return []
  } else {
    if (typeof ua === 'string') return letterToNum(ua)
    return null
  }
})

const formatUserAnswerText = (q) => {
  const ua = q.userAnswer
  if (ua === null || ua === undefined) return '（未作答）'
  if (q.type === 0) return (ua === true || ua === 1) ? '正确' : '错误'
  if (q.type === 1) {
    if (Array.isArray(ua)) return ua.join('、')
    return String(ua)
  }
  const t = stripHtml(ua)
  return t.length ? t : '（未作答）'
}

const formatCorrectAnswerText = (q) => {
  const ca = q.correctAnswer
  if (q.type === 0) return (ca === true) ? '正确' : '错误'
  if (q.type === 1) {
    if (Array.isArray(ca)) return ca.join('、')
    return String(ca || '')
  }
  return ''
}

const splitKeywords = (kw) => {
  if (!kw) return []
  return String(kw)
    .split(/[,，、\s]+/)
    .map(s => s.trim())
    .filter(Boolean)
}

const isCorrect = (q) => {
  if (q.userScore!==q.score) return false
  return true
}

const scoreTagType = (q) => {
  const us = q?.userScore ?? 0
  const s = q?.score ?? 0
  if (s === 0) return 'info'
  if (us === s) return 'success'
  if (us === 0) return 'danger'
  return 'warning'
}

const viewReport = async () => {
  const loading = ElLoading.service({
      lock: true,
      text: '正在生成本次测试能力诊断报告，请稍候...',
      background: 'rgba(0, 0, 0, 0.7)'
  })
  try {
    const result = await createFullReportService(qnId)
    ElMessage.success('生成个性化能力诊断报告成功...')
    const rId = result.data.id
    router.push({ path: '/report/detail', query: { id: rId} })
  } catch (e) {
    console.error(e)
    ElMessage.error('生成报告失败，请稍后重试')
  } finally {
    loading.close()
  }
}

const goOut = () => {
    ElMessageBox.confirm(
        '确认要退出该测试详情吗？',
        '温馨提示', 
        { confirmButtonText: '确认退出', cancelButtonText: '取消', type: 'warning' }
    ).then(async () => {
        ElMessage.success('成功退出！')
        router.back()
    }).catch(() => {
        ElMessage.info('继续查看！')
    })
}

onMounted(async () => {
    if (!qnId) {
        ElMessage.warning('请选择测试情景')
        router.push('/test/home')
        return
    }
    try {
        const res = await getResultService(qnId)
        resultDTO.value = res.data
        transformResult(resultDTO.value.questionResults || [])
        // 默认展示第一题
        if (topic.value[0].length) judge(0)
        else if (topic.value[1].length) change(0)
        else if (topic.value[2].length) fill(0)
    } catch (e) {
        console.error(e)
        ElMessage.error(e?.message || '加载答卷详情失败')
        router.push('/test/home')
    }
})
</script>

<template>
    <div style="display: flex;">
        <transition name="slider-fade">
            <div class="left">
                <el-card class="timer-card" shadow="never" :body-style="{padding: '10px'}">
                    <div class="timer-text"> 用时：{{ formatSpent(resultDTO?.timeSpent) }} </div>
                </el-card>
                <el-card class="question-card" shadow="never" :body-style="{padding: '10px'}">
                    <div class="l-top">
                        <div class="item" v-if="topicCount[0]> 0">
                            <p>判断题部分</p>
                            <ul>
                                <li v-for="(question, idx) in topic[0]" :key="question.id">
                                    <a href="javascript:;" @click="judge(idx)" 
                                        :class="{'border': index === idx && currentType === 0, 'bg': question.isClick === true}">
                                        <span :class="{'mark': question.isMark === true}"></span>
                                        {{idx+1}}
                                    </a>
                                </li>
                            </ul>
                        </div>
                        <div class="item" v-if="topicCount[1] > 0">
                            <p>选择题部分</p>
                            <ul>
                                <li v-for="(question, idx) in topic[1]" :key="question.id">
                                    <a href="javascript:;" @click="change(idx)"
                                        :class="{'border': index === idx && currentType === 1,'bg': question.isClick === true}">
                                        <span :class="{'mark': question.isMark === true}"></span>
                                        {{topicCount[0]+idx+1}}
                                    </a>
                                </li>
                            </ul>
                        </div>
                        <div class="item" v-if="topicCount[2] > 0">
                            <p>简答题部分</p>
                            <ul>
                                <li v-for="(question, idx) in topic[2]" :key="question.id">
                                    <a href="javascript:;" @click="fill(idx)" 
                                        :class="{'border': index === idx && currentType === 2,'bg': question.isClick === true}">
                                        <span :class="{'mark': question.isMark === true}"></span>
                                        {{topicCount[0]+topicCount[1]+idx+1}}
                                    </a>
                                </li>
                            </ul>
                        </div>
                    </div>
                    <ul class="l-bottom">
                        <li><a href="javascript:;"></a><span>当前</span></li>
                        <li><a href="javascript:;"></a><span>未答</span></li>
                        <li><a href="javascript:;"></a><span>已答</span></li>
                        <li><a href="javascript:;"></a><span>标记</span></li>
                    </ul>
                    <el-button @click="goOut()" type="primary" plain size="large" class="btn">返回上一页</el-button>
                    <el-button @click="viewReport()" type="primary" size="large" class="btn">查看本次诊断报告</el-button>
                </el-card>
            </div>
        </transition>

        <transition name="slider-fade">
            <div class="right">
                <el-card class="title" shadow="never" :body-style="{padding: '10px'}">
                    <div class="title-text">
                        <div style="margin: 0 0 0 20px;">{{title}}</div>
                        <div class="paper-meta">
                            <span class="paper-title">{{ resultDTO?.title || '情景测试' }}</span>
                            <span>｜全卷共{{ totalQuestions }}题</span>
                            <span>｜得分：{{ resultDTO?.userTotalScore }} / {{ resultDTO?.totalScore}} 分</span>
                        </div>
                    </div>
                </el-card>
                <el-card class="content" :body-style="{padding: '10px'}">
                    <p class="topic">
                        <span class="number">{{ number }}</span>
                        <span class="score-flag">（{{ showQuestion.score }}分）</span>
                        <span class="flag" v-if="currentType === 1 && showQuestion.isMultiple">（多选）</span>
                        {{ showQuestion.content }}
                    </p>
                    <div class="judge" v-if="currentType == 0">
                        <el-radio-group v-model="judgeModelValue">
                            <el-radio :label="1">正确</el-radio>
                            <el-radio :label="2">错误</el-radio>
                        </el-radio-group>
                    </div>
                    <div v-if="currentType === 1 && !showQuestion.isMultiple">
                        <el-radio-group v-model="choiceModelValue">
                            <el-radio :label="1">A. {{ showQuestion.optionA }}</el-radio>
                            <el-radio :label="2">B. {{ showQuestion.optionB }}</el-radio>
                            <el-radio :label="3">C. {{ showQuestion.optionC }}</el-radio>
                            <el-radio :label="4">D. {{ showQuestion.optionD }}</el-radio>
                        </el-radio-group>
                    </div>
                    <div v-if="currentType === 1 && showQuestion.isMultiple">
                        <el-checkbox-group  v-model="choiceModelValue">
                            <el-checkbox :label="1">A. {{ showQuestion.optionA }}</el-checkbox>
                            <el-checkbox :label="2">B. {{ showQuestion.optionB }}</el-checkbox>
                            <el-checkbox :label="3">C. {{ showQuestion.optionC }}</el-checkbox>
                            <el-checkbox :label="4">D. {{ showQuestion.optionD }}</el-checkbox>
                        </el-checkbox-group>
                    </div>
                    <div class="fill" v-if="currentType == 2">
                        <quill-editor 
                            v-model:content="showQuestion.userAnswer"
                            contentType="html" 
                            read-only
                            theme="snow"
                        />
                    </div>
                    <div class="result-panel">
                        <div class="summary">
                            <div class="summary-left">
                                <el-tag size="large" :type="scoreTagType(showQuestion)">
                                    得分 {{ showQuestion.userScore ?? 0 }} / {{ showQuestion.score }} 分
                                </el-tag>
                                <el-tag class="ml8" size="large" effect="plain" type="info">
                                    难度 {{ showQuestion.difficulty ?? '-' }}
                                </el-tag>
                            </div>
                            <div class="summary-right" v-if="showQuestion.type !== 2">
                                <el-tag :type="isCorrect(showQuestion) ? 'success' : 'danger'" size="large">
                                    {{ isCorrect(showQuestion) ? '回答正确' : '回答错误' }}
                                </el-tag>
                            </div>
                        </div>
                        <el-card class="block-card" shadow="never">
                            <template #header>
                                <div class="card-title">
                                    <span>作答信息</span>
                                </div>
                            </template>
                            <el-descriptions :column="1" border>
                                <el-descriptions-item label="我的回答">
                                    <div class="ans-text">{{ formatUserAnswerText(showQuestion) }}</div>
                                </el-descriptions-item>

                                <el-descriptions-item v-if="showQuestion.type !== 2" label="正确答案">
                                    <el-tag type="success" effect="dark">{{ formatCorrectAnswerText(showQuestion) }}</el-tag>
                                </el-descriptions-item>
                                <el-descriptions-item label="本题得分">
                                    <span class="score-strong">{{ showQuestion.userScore ?? 0 }}</span>
                                    <span class="score-muted"> / {{ showQuestion.score }} 分</span>
                                </el-descriptions-item>
                            </el-descriptions>
                        </el-card>
                        <el-alert class="analysis-alert"  :title="showQuestion.analysis ? '解析：' + showQuestion.analysis : '解析：暂无'"
                            type="info" :closable="false" show-icon
                        />
                        <template v-if="showQuestion.type === 2">
                            <el-card class="block-card" shadow="never">
                                <template #header>
                                    <div class="card-title">
                                        <span>参考与关键点</span>
                                    </div>
                                </template>
                                <el-descriptions :column="1" border>
                                    <el-descriptions-item label="参考答案">
                                        <div class="ref-text"> {{ showQuestion.reference || '—' }}</div>
                                    </el-descriptions-item>
                                    <el-descriptions-item label="关键点">
                                        <div class="kw-wrap">
                                            <el-tag v-for="(k, i) in splitKeywords(showQuestion.keyword)":key="i"
                                                type="warning" effect="plain" class="kw-tag"> {{ k }} </el-tag>
                                            <span v-if="splitKeywords(showQuestion.keyword).length === 0">—</span>
                                        </div>
                                    </el-descriptions-item>
                                </el-descriptions>
                            </el-card>
                        </template>
                    </div>
                </el-card>
                <el-card class="operation" shadow="never" :body-style="{padding: '10px'}">
                    <ul class="end">
                        <li @click="previous()"><span><el-icon><ArrowLeft /></el-icon>上一题</span></li>
                        <li><span>标记</span></li>
                        <li @click="next()"><span>下一题<el-icon><ArrowRight /></el-icon></span></li>
                    </ul>
                </el-card>
            </div>
        </transition>
    </div>
</template>

<style lang="scss" scoped>
.result-panel{
  margin-top: 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  .summary{
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 10px 10px;
    background: #f8fafc;
    border: 1px solid #eef2f7;
    border-radius: 12px;
  }
  .ml8{ margin-left: 8px; }
}
.block-card{
  border-radius: 12px;
  .card-title{
    font-weight: 800;
    display: flex;
    align-items: center;
    justify-content: space-between;
  }
  .ans-text{
    font-size: 15px;
    line-height: 1.75;
    color: #111827;
    white-space: pre-wrap;
  }
}
.ref-text{
  line-height: 1.75;
  color: #111827;
  white-space: pre-wrap;
}

.kw-wrap{
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}

.kw-tag{
  border-radius: 999px;
}

.score-strong{
  font-size: 18px;
  font-weight: 900;
}

.score-muted{
  color: #6b7280;
}

.analysis-alert{
  border-radius: 12px;
}

.fill {
  width: 100%;
  :deep(.ql-editor) {
    min-height: 120px;
  }
}
.end{
    display: flex;
    justify-content: center;
    align-items: center;
    color: rgb(64,158,255);
    margin: 0;
    padding: 0;
    li {
        cursor: pointer;
        margin: 0 100px;
        display: flex;
    }
    li:nth-child(2) {
        display: flex;
        flex-direction: column;
        justify-content: center;
        align-items: center;
        background-color: rgb(64,158,255);
        border-radius: 50%;
        width: 50px;
        height: 50px;
        color: #fff;
    }
}
.right {
    flex: 1;
    .title{
        margin-bottom: 20px;
        .title-text {
            font-size: 18px;
            display: flex;
            justify-content: space-between;
            align-items: center;
            .paper-meta{
                margin-right: 20px;
                .paper-title{
                    font-weight: 700;
                }
            }
        }
    }
    .content{
        margin-bottom: 20px;
        min-height: 460px;
        .number {
            display: inline-flex;
            justify-content: center;
            align-items: center;
            width: 25px;
            height: 25px;
            background-color: rgb(64,158,255);
            border-radius: 4px;
            margin-right: 5px;
            color: #fff;
        }
        .topic {
            margin: 10px 20px 10px 20px;
            .flag{
                color: #000000;
                font-weight: 700;
            }
            .score-flag{
                color: #6b7280;
            }
        }
        .el-radio-group {
            display: flex;
            flex-direction: column;
            align-items: flex-start;
            margin: 10px 20px 10px 50px;
            label {
                color: #000;
                margin: 5px 0px;
                font-size: 18px;
            }
        }
        :deep(.el-checkbox-group){
            display: flex;
            flex-direction: column;
            align-items: flex-start;
            margin: 10px 20px 10px 50px;
        }
        :deep(.el-checkbox){
            margin: 5px 0;
            font-size: 18px;
            color: #000;
        }
    }
    .operation {
        margin-top: 0px;
    }
}

.border {
  position: relative;
  border: 1px solid #FF90AA !important;
}
.bg {
  background-color: #51b86d !important;
}
.mark {
  position: absolute;
  width: 4px;
  height: 4px;
  content: "";
  background-color: red;
  border-radius: 50%;
  top: 0px;
  left: 22px;
}

.l-top {
    padding: 0px;
    border: 1px solid #eee;
    border-radius: 4px;
    margin-top: 10px;
    margin-bottom: 20px;
    background-color: #f7f7f7;
    min-height: 320px;
    .item {
        display: flex;
        flex-direction: column;
        p {
            margin-bottom: 15px;
            margin-top: 10px;
            color: #000;
            margin-left: 10px;
            letter-spacing: 2px;
        }
        ul {
            width: 100%;
            margin-bottom: -8px;
            display: flex;
            justify-content: flex-start;
            gap: 10px; 
            flex-wrap: wrap;
            padding: 0;
            margin-left: 0;
            margin-top: 0;
        }
        li {
            width: 18%;
            margin: 0px 0px 10px 8px;
            display: flex;
            a { 
                text-decoration: none;
                position: relative;
                justify-content: center;
                display: inline-flex;
                align-items: center;
                width: 30px;
                height: 30px;
                border-radius: 50%;
                background-color: #fff;
                border: 1px solid #eee;
                text-align: center;
                color: #000;
                font-size: 16px;
            }
        }
    }
}
.l-bottom {
    display: flex;
    justify-content: space-around;
    padding: 7px 0px;
    border: 1px solid #eee;
    border-radius: 4px;
    margin-top: 0px;
    margin-bottom: 20px;
    background-color: #fff;
    li {
        display: flex;
        justify-content: center;
        align-items: center;
        flex-direction: column;
        a {
            display: inline-block;
            padding: 10px;
            border-radius: 50%;
            background-color: #fff;
            border: 1px solid #FF90AA;
        }
    }
    li:nth-child(2) a {
        border: 1px solid #eee;
    }
    li:nth-child(3) a {
        background-color: #51b86d;
        border: none;
    }
    li:nth-child(4) a {
        position: relative;
        border: 1px solid #eee;
    }
    li:nth-child(4) a::before {
        width: 4px;
        height: 4px;
        content: " ";
        position: absolute;
        background-color: red;
        border-radius: 50%;
        top: 0px;
        left: 16px;
    }
}
.btn {
    width: 100%;
    padding: 0px;
    margin-top: 0px;
    margin-bottom: 10px;
    margin-left: 0px;
}
.left {
    width: 260px;
    height: 100%;
    margin-right: 20px;
    .timer-card {
        text-align: center;
        margin-bottom: 20px;
        .timer-text {
            font-size: 18px;
        }
    }
    .question-card {
        .question-card-header {
            font-size: 18px;
            text-align: center;
        }
    }
}

/* slider过渡效果 */
.slider-fade-enter-active {
  transition: all .3s ease;
}
.slider-fade-leave-active {
  transition: all .3s cubic-bezier(1.0, 0.5, 0.8, 1.0);
}
.slider-fade-enter, .slider-fade-leave-to {
  transform: translateX(-100px);
  opacity: 0;
}
</style>