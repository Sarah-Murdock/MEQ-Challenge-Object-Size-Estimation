package com.example.meqchallenge

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class ChallengeFragmentViewModel: ViewModel() {

    val referenceDetected: MutableLiveData<Boolean> = MutableLiveData(false)
    val objectDetected: MutableLiveData<Boolean> = MutableLiveData(false)
    val referenceSizeCorrect: MutableLiveData<Boolean> = MutableLiveData(false)

    val objectLabel = MutableLiveData<String>()
    val objectEstimatedWidthCm = MutableLiveData<Float>()
    val objectEstimatedHeightCm = MutableLiveData<Float>()
    val objectEstimatedWidthScale = MutableLiveData<Float>()
    val objectEstimatedHeightScale = MutableLiveData<Float>()
}