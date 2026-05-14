<script setup>
import { ArrowLeft, ArrowRight} from '@element-plus/icons-vue'
import {ElMessage,ElMessageBox, ElLoading } from 'element-plus'
import { QuillEditor } from '@vueup/vue-quill'
import '@vueup/vue-quill/dist/vue-quill.snow.css'

import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { getQuestionsService, submitService, abandonService } from '@/api/test.js'
import { useRouter, onBeforeRouteLeave } from 'vue-router'
const router = useRouter()
const qnId = router.currentRoute.value.query.id
const quizTitle = router.currentRoute.value.query.title
const totalScore = Number(router.currentRoute.value.query.totalScore)
const totalCount = Number(router.currentRoute.value.query.totalCount)

const timeInSeconds = ref(0)   // 用秒数来计时
const time = computed(() => {  // 格式化计时器时间为 HH:mm:ss
  const hours = String(Math.floor(timeInSeconds.value / 3600)).padStart(2, '0');
  const minutes = String(Math.floor((timeInSeconds.value % 3600) / 60)).padStart(2, '0');
  const seconds = String(timeInSeconds.value % 60).padStart(2, '0');
  return `${hours}:${minutes}:${seconds}`;
})
let timer = null

const title = ref('请选择正确的选项')
const currentType = ref(0)
const topicCount = ref([0, 0, 0])
const topic = ref({ 
    0: [], // 判断题
    1: [], // 选择题
    2: []  // 简答题
})

// 将后端题目数据转换为前端格式
const transformQuestions = (questions) => {
    let judgeIndex = 0;
    let changeIndex = 0;
    let fillIndex = 0;
    
    questions.forEach(q => {
        if (q.type === 0) { // 判断题
            topic.value[0].push({
                id: q.id,
                content: q.content,
                difficulty: q.difficulty,
                score: q.score,              
                isClick: false,
                isMark: false,
                index: judgeIndex++
            });
        } else if (q.type === 1) { // 选择题
            topic.value[1].push({
                id: q.id,
                content: q.content,
                difficulty: q.difficulty,
                score: q.score,
                optionA: q.optionA,
                optionB: q.optionB,
                optionC: q.optionC,
                optionD: q.optionD,
                isMultiple: q.isMultiple,
                isClick: false,
                isMark: false,
                index: changeIndex++
            });
        } else if (q.type === 2) { // 简答题
            topic.value[2].push({
                id: q.id,
                content: q.content,
                difficulty: q.difficulty,
                score: q.score,
                isClick: false,
                isMark: false,
                index: fillIndex++
            });
        }
    })
    topicCount.value[0] = topic.value[0].length
    topicCount.value[1] = topic.value[1].length
    topicCount.value[2] = topic.value[2].length
}

const bg_flag = ref(false); //已答标识符,已答改变背景色
const index = ref(0); //当前题目索引，全局index
const number = ref(1); // 当前题号
const showQuestion = ref({}); // 当前显示的题目

const changeAnswer = ref([]); //保存所有选择题的回答
const judgeAnswer = ref([]); // 保存所有判断题的回答
const fillAnswer = ref([]); // 保存所有简答题的回答

// ===== 切题 =====
const judge = (idx) => {
  currentType.value = 0;
  index.value = idx;
  if(idx < topic.value[0].length){
    if(idx <= 0){
        index.value = 0;
    }
    title.value = '请作出正确判断';
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
        title.value = '请选择正确的答案';
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
        title.value = '请在文字框内输入答案';
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

// 标记功能
const mark = () => {
    const q = topic.value[currentType.value][index.value]
    q.isMark = !q.isMark
}

// 获取判断题作答选项
const getJudgeAnswer = (val) => {
  judgeAnswer.value[index.value] = val;
  if (val !== undefined && val !== null) {
    bg_flag.value = true
    topic.value[0][index.value].isClick = true
  }
}
// 获取选择题作答选项
const getChangeAnswer = (val) => {
  changeAnswer.value[index.value] = val
  if (val !== undefined && val !== null && !(Array.isArray(val) && val.length === 0)) {
    bg_flag.value = true
    topic.value[1][index.value].isClick = true
  }
}
// 获取简答题作答
const fillKey = computed(() => `essay-${index.value}`)
const stripHtml = (html) => {
  if (html === null || html === undefined) return ''
  return html.replace(/<br\s*\/?>/gi, '').replace(/<\/?p[^>]*>/gi, '').replace(/<\/?div[^>]*>/gi, '')
    .replace(/&nbsp;/gi, ' ').replace(/<[^>]+>/g, '').trim()
}
const getFillAnswer = (val) => {
  fillAnswer.value[index.value] = val;
  const ok = stripHtml(val).length > 0
  if (ok) {
    bg_flag.value = true
    topic.value[2][index.value].isClick = true
  }
}

const submitted = ref(false) // 是否已提交（用于路由离开拦截）
const bypassLeaveGuard = ref(false) // 用户主动退出时放行路由（避免二次弹窗）


const isAnswered = (type, idx) => {
  if (type === 0) return judgeAnswer.value[idx] !== undefined && judgeAnswer.value[idx] !== null
  if (type === 1) {
    const a = changeAnswer.value[idx]
    if (Array.isArray(a)) return a.length > 0
    return a !== undefined && a !== null && a !== ''
  }
  if (type === 2) {
    const a = fillAnswer.value[idx]
    return stripHtml(a).length > 0
  }
  return false
}
const answeredCount = computed(() => {
  let c = 0
  for (let i = 0; i < topicCount.value[0]; i++) if (isAnswered(0, i)) c++
  for (let i = 0; i < topicCount.value[1]; i++) if (isAnswered(1, i)) c++
  for (let i = 0; i < topicCount.value[2]; i++) if (isAnswered(2, i)) c++
  return c
})

const progressPercent = computed(() => {
  return Math.round((answeredCount.value / totalCount) * 100)
})

const goOut = () => {
    ElMessageBox.confirm(
        '确认要退出答题吗？退出后本次测试将作废（记录会被删除）。',
        '温馨提示', 
        { confirmButtonText: '确认退出', cancelButtonText: '继续答题', type: 'warning' }
    ).then(async () => {
        try {
            bypassLeaveGuard.value = true

            await abandonService(qnId)
            console.log(qnId)
            ElMessage.success('已退出，本次测试已作废')
            router.push('/test/home')
        } catch (e) {
            console.error(e)
            ElMessage.error(e?.message || '退出失败，请稍后重试')
            bypassLeaveGuard.value = false
        }
    }).catch(() => {
        ElMessage.info('继续测试！')
    })
}

const numToLetter = (n) => String.fromCharCode(64 + Number(n)) // 1->A
const buildSubmitPayload = () => {
  const answers = []
  // 判断：保持 1/2（后端按 1=true 处理）
  topic.value[0].forEach((q, idx) => {
    const ua = judgeAnswer.value[idx]
    if (ua === undefined || ua === null) return
    answers.push({
      questionId: q.id,
      userAnswer: Number(ua)===1, 
      isMarked: q.isMark === true
    })
  })
  // 选择:答题时保持数字/数字数组,提交时再转字母
  topic.value[1].forEach((q, idx) => {
    const ua = changeAnswer.value[idx]
    if (ua === undefined || ua === null || (Array.isArray(ua) && ua.length === 0)) return

    let userAnswer
    if (q.isMultiple) {
      userAnswer = Array.isArray(ua) ? ua.map(numToLetter) : []
    } else {
      userAnswer = numToLetter(ua)
    }
    answers.push({
      questionId: q.id,
      userAnswer,
      isMarked: q.isMark === true
    })
  })
  // 简答：原样提交（html 字符串也行）
  topic.value[2].forEach((q, idx) => {
    const ua = fillAnswer.value[idx]
    if (ua === undefined || ua === null || String(ua).trim() === '') return
    answers.push({
      questionId: q.id,
      userAnswer: ua,
      isMarked: q.isMark === true
    })
  })
  return {
    timeSpent: timeInSeconds.value,
    answers
  }
}

const commit = () => {
    const tip =answeredCount.value < totalCount
        ? `还有题目未作答。\n\n是否仍要提交？`
        : '确认要提交测试吗?'
    ElMessageBox.confirm(tip, '温馨提示', {
        confirmButtonText: '确认',
        cancelButtonText: '取消',
        type: 'warning',
    }).then(async () => {
        const loading = ElLoading.service({
            lock: true,
            text: '正在提交并自动评分，请耐心等待...',
            background: 'rgba(0, 0, 0, 0.45)'
        })
        try {
            const payload = buildSubmitPayload()
            console.log(payload)
            const result = await submitService(qnId, payload);
            console.log('提交结果:', result);

            submitted.value = true
            ElMessage.success('提交成功!')
            router.push({ path: '/test/result',  query: { id: qnId } });
        } catch (error) {
            console.error('提交失败:', error);
            ElMessage.error(e.message || '提交失败，请稍后重试')
        } finally {
            loading.close()
        }
    }).catch(() => {
        ElMessage.info('继续测试！')
    })
}

onMounted(async () => {
    if (!qnId) {
        ElMessage.warning('请选择测试情景')
        router.push('/test/home')
        return
    }
    try {
        // 拉题：GET /test/quiz/questions
        const qResult = await getQuestionsService(qnId)
        transformQuestions(qResult.data);
        
        // 初始化用户答案数组
        judgeAnswer.value = new Array(topicCount.value[0])
        changeAnswer.value = new Array(topicCount.value[1])
        fillAnswer.value = new Array(topicCount.value[2])
        
        // 默认展示第一题
        if (topic.value[0].length) judge(0)
        else if (topic.value[1].length) change(0)
        else if (topic.value[2].length) fill(0)
        // 启动计时器
        timer = setInterval(() => { timeInSeconds.value += 1 }, 1000)
    } catch (e) {
        console.error(e);
        ElMessage.error(e?.message || '题目加载失败，请稍后重试')
        router.push('/test/home');
    }
})

onBeforeUnmount(() => {
    if (timer) {
        clearInterval(timer);
    }
})

onBeforeRouteLeave(async (to, from, next) => {
  // 已提交或用户主动退出（goOut确认后）则放行
  if (submitted.value || bypassLeaveGuard.value) return next()

  try {
    await ElMessageBox.confirm(
      '当前答卷尚未提交，确定要退出吗？',
      '离开提示',
      { confirmButtonText: '退出', cancelButtonText: '继续答题', type: 'warning' }
    )
    next()
  } catch {
    next(false)
  }
})
</script>

<template>
    <div style="display: flex;">
        <transition name="slider-fade">
            <div class="left">
                <el-card class="timer-card" shadow="never" :body-style="{padding: '10px'}">
                    <div class="timer-text">用时: {{ time }}</div>
                </el-card>
                <el-card class="question-card" shadow="never" :body-style="{padding: '10px'}">
                    <div class="l-top">
                        <div class="item" v-if="topicCount[0]> 0">
                            <p>判断题部分</p>
                            <ul>
                                <li v-for="(question, idx) in topic[0]" :key="idx">
                                    <a href="javascript:;" @click="judge(idx)" 
                                        :class="{'border': index === idx && currentType === 0, 'bg': bg_flag && question.isClick === true}">
                                        <span :class="{'mark': question.isMark === true}"></span>
                                        {{idx+1}}
                                    </a>
                                </li>
                            </ul>
                        </div>
                        <div class="item" v-if="topicCount[1] > 0">
                            <p>选择题部分</p>
                            <ul>
                                <li v-for="(question, idx) in topic[1]" :key="idx">
                                    <a href="javascript:;" @click="change(idx)"
                                        :class="{'border': index === idx && currentType === 1,'bg': bg_flag && question.isClick === true}">
                                        <span :class="{'mark': question.isMark === true}"></span>
                                        {{topicCount[0]+idx+1}}
                                    </a>
                                </li>
                            </ul>
                        </div>
                        <div class="item" v-if="topicCount[2] > 0">
                            <p>简答题部分</p>
                            <ul>
                                <li v-for="(question, idx) in topic[2]" :key="idx">
                                    <a href="javascript:;" @click="fill(idx)" 
                                        :class="{'border': index === idx && currentType === 2,'bg': bg_flag && question.isClick === true}">
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
                    <el-button @click="goOut()" type="primary" plain size="large" class="btn">退出答题</el-button>
                    <el-button @click="commit()" type="primary" size="large" class="btn">提交测试</el-button>
                </el-card>
            </div>
        </transition>

        <transition name="slider-fade">
            <div class="right">
                <el-card class="title" shadow="never" :body-style="{padding: '10px'}">
                    <div class="title-text">
                        <div style="margin: 0 0 0 20px;">{{title}}</div>
                        <div class="paper-meta">
                            <span class="paper-title">{{ quizTitle || '情景测试' }}</span>
                            <span>｜全卷共{{ totalCount }}题</span>
                            <span>｜总分{{ totalScore || 140 }}分</span>
                        </div>
                    </div>
                </el-card>
                <el-card class="title" shadow="never" :body-style="{padding: '10px'}">
                    <div class="progress-text">已答 {{ answeredCount }} / {{ totalCount }}</div>
                    <el-progress :percentage="progressPercent" :stroke-width="8" />
                </el-card>
                <el-card class="content" :body-style="{padding: '10px'}">
                    <p class="topic">
                        <span class="number">{{ number }}</span>
                        <span class="score-flag">（{{ showQuestion.score }}分）</span>
                        <span class="flag" v-if="currentType === 1 && showQuestion.isMultiple">（多选）</span>
                        {{ showQuestion.content }}
                    </p>
                    <div class="judge" v-if="currentType == 0">
                        <el-radio-group v-model="judgeAnswer[index]" @change="getJudgeAnswer">
                            <el-radio :label="1">正确</el-radio>
                            <el-radio :label="2">错误</el-radio>
                        </el-radio-group>
                    </div>
                    <div v-if="currentType === 1 && !showQuestion.isMultiple">
                        <el-radio-group v-model="changeAnswer[index]" @change="getChangeAnswer">
                            <el-radio :label="1">A. {{ showQuestion.optionA }}</el-radio>
                            <el-radio :label="2">B. {{ showQuestion.optionB }}</el-radio>
                            <el-radio :label="3">C. {{ showQuestion.optionC }}</el-radio>
                            <el-radio :label="4">D. {{ showQuestion.optionD }}</el-radio>
                        </el-radio-group>
                    </div>
                    <div v-if="currentType === 1 && showQuestion.isMultiple">
                        <el-checkbox-group  v-model="changeAnswer[index]" @change="getChangeAnswer">
                            <el-checkbox :label="1">A. {{ showQuestion.optionA }}</el-checkbox>
                            <el-checkbox :label="2">B. {{ showQuestion.optionB }}</el-checkbox>
                            <el-checkbox :label="3">C. {{ showQuestion.optionC }}</el-checkbox>
                            <el-checkbox :label="4">D. {{ showQuestion.optionD }}</el-checkbox>
                        </el-checkbox-group>
                    </div>
                    <div class="fill" v-if="currentType == 2">
                        <quill-editor :key="fillKey"
                            v-model:content="fillAnswer[index]" 
                            contentType="html" 
                            :options="{placeholder: '请在此处输入答案'}" 
                            @update:content="getFillAnswer"/>
                    </div>
                </el-card>
                <el-card class="operation" shadow="never" :body-style="{padding: '10px'}">
                    <ul class="end">
                        <li @click="previous()"><span><el-icon><ArrowLeft /></el-icon>上一题</span></li>
                        <li @click="mark()"><span>标记</span></li>
                        <li @click="next()"><span>下一题<el-icon><ArrowRight /></el-icon></span></li>
                    </ul>
                </el-card>
            </div>
        </transition>
    </div>
</template>

<style lang="scss" scoped>
.fill {
  width: 100%;
  :deep(.ql-editor) {
    min-height: 350px;
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
        .progress-text{
            margin-top: 3px;
            font-size: 13px;
            color: #6b7280;
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
            margin: 10px 20px 10px 50px;  // 与 radio 的缩进保持一致
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
    // display: inline-block;
    // flex-direction: column;
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
        // height: 400px;
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