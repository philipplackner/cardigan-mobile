package com.plcoding.cardiganapp.feature.transfer.presentation.fakes

import com.plcoding.cardiganapp.feature.transfer.presentation.platform.FilePicker
import com.plcoding.cardiganapp.feature.transfer.presentation.platform.PickedFile

class FakeFilePicker : FilePicker {
    var openCount = 0
        private set
    private var onPicked: (PickedFile) -> Unit = {}
    private var onCancelled: () -> Unit = {}

    fun bind(onPicked: (PickedFile) -> Unit, onCancelled: () -> Unit): FakeFilePicker {
        this.onPicked = onPicked
        this.onCancelled = onCancelled
        return this
    }

    override fun pickJson() {
        openCount++
    }

    fun pick(file: PickedFile) = onPicked(file)

    fun cancel() = onCancelled()
}
