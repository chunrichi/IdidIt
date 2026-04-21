package com.example.ididit.data.repository

import com.example.ididit.data.local.TopicDao
import com.example.ididit.data.local.TopicEntity
import kotlinx.coroutines.flow.Flow

class TopicRepository(private val topicDao: TopicDao) {
    fun getAllTopics(): Flow<List<TopicEntity>> = topicDao.getAllTopics()

    suspend fun getTopicById(id: Long): TopicEntity? = topicDao.getTopicById(id)

    suspend fun insert(topic: TopicEntity): Long = topicDao.insert(topic)

    suspend fun update(topic: TopicEntity) = topicDao.update(topic)

    suspend fun delete(topic: TopicEntity) = topicDao.delete(topic)
}
