package com.ethersense.domain.usecase

import com.ethersense.data.model.SpeedTestProgress
import com.ethersense.data.model.SpeedTestResult
import com.ethersense.data.repository.SpeedTestRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class RunSpeedTestUseCase @Inject constructor(
    private val speedTestRepository: SpeedTestRepository
) {
    fun observeProgress(): Flow<SpeedTestProgress> {
        return speedTestRepository.runSpeedTest()
    }

    suspend fun runTest(): SpeedTestResult {
        return speedTestRepository.runFullSpeedTest()
    }
}
