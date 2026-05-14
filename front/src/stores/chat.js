import { defineStore } from 'pinia'
import {ref} from 'vue'

const useChatStore = defineStore('chat',()=>{
    //聊天记录
    const chatMessages = ref([])

    //添加一条消息
    const addMessage = (message)=>{
        chatMessages.value.push(message)
    }

    //清空消息
    const clearMessages = ()=>{
        chatMessages.value = []
    }

    return {
        chatMessages,addMessage,clearMessages
    }
},{
    persist:true//持久化存储
});

export default useChatStore