package com.unshoo.pixelmusic.data.service.player

import androidx.media3.common.Player
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class DualPlayerEnginePolicyTest {

    @Test
    fun transitionRunTracker_invalidatesPreviousRunWhenNewRunStarts() {
        val tracker = TransitionRunTracker()

        val firstRun = tracker.start()
        val secondRun = tracker.start()

        assertThat(tracker.isCurrent(firstRun)).isFalse()
        assertThat(tracker.isCurrent(secondRun)).isTrue()
    }

    @Test
    fun transitionRunTracker_invalidationMakesCurrentRunStale() {
        val tracker = TransitionRunTracker()
        val run = tracker.start()

        tracker.invalidate()

        assertThat(tracker.isCurrent(run)).isFalse()
    }

    @Test
    fun rebuildUsesPresentedAuxiliaryOnlyDuringActiveTransitionWithMedia() {
        assertThat(
            shouldUsePresentedAuxiliaryForPlayerRebuild(
                transitionRunning = true,
                auxiliaryPlayerPresented = true,
                auxiliaryMediaItemCount = 1
            )
        ).isTrue()

        assertThat(
            shouldUsePresentedAuxiliaryForPlayerRebuild(
                transitionRunning = false,
                auxiliaryPlayerPresented = true,
                auxiliaryMediaItemCount = 1
            )
        ).isFalse()

        assertThat(
            shouldUsePresentedAuxiliaryForPlayerRebuild(
                transitionRunning = true,
                auxiliaryPlayerPresented = true,
                auxiliaryMediaItemCount = 0
            )
        ).isFalse()
    }

    @Test
    fun rebuildQueueWindowFollowsThePlayerThatWillBecomeMaster() {
        val activeWindow = selectPlayerRebuildQueueWindow(
            useAuxiliaryPlayer = false,
            activeWindowStartIndex = 4,
            activePlayerUsesWindowedQueue = true,
            preparedWindowStartIndex = 8,
            preparedPlayerUsesWindowedQueue = false
        )
        val presentedAuxiliaryWindow = selectPlayerRebuildQueueWindow(
            useAuxiliaryPlayer = true,
            activeWindowStartIndex = 4,
            activePlayerUsesWindowedQueue = true,
            preparedWindowStartIndex = 8,
            preparedPlayerUsesWindowedQueue = false
        )

        assertThat(activeWindow.startIndex).isEqualTo(4)
        assertThat(activeWindow.usesWindowedQueue).isTrue()
        assertThat(presentedAuxiliaryWindow.startIndex).isEqualTo(8)
        assertThat(presentedAuxiliaryWindow.usesWindowedQueue).isFalse()
    }

    @Test
    fun transientFocusLossResumesWhenEitherTransitionPlayerWasActive() {
        assertThat(
            shouldResumeAfterTransientAudioFocusLoss(
                masterPlayWhenReady = false,
                masterIsPlaying = false,
                transitionRunning = true,
                auxiliaryPlayWhenReady = true,
                auxiliaryIsPlaying = false
            )
        ).isTrue()

        assertThat(
            shouldResumeAfterTransientAudioFocusLoss(
                masterPlayWhenReady = false,
                masterIsPlaying = false,
                transitionRunning = false,
                auxiliaryPlayWhenReady = true,
                auxiliaryIsPlaying = true
            )
        ).isFalse()
    }

    @Test
    fun offloadStallFallbackOnlyTriggersForStalledCurrentMasterOutsideTransition() {
        val common = mapOf(
            "audioOffloadEnabled" to true,
            "transitionRunning" to false,
            "isCurrentMasterPlayer" to true,
            "mediaIdMatches" to true,
            "playbackState" to Player.STATE_READY,
            "isPlaying" to false,
            "playWhenReady" to true,
            "playbackSuppressionReason" to Player.PLAYBACK_SUPPRESSION_REASON_NONE
        )

        assertThat(
            shouldTriggerAudioOffloadStallFallback(
                audioOffloadEnabled = common.getValue("audioOffloadEnabled") as Boolean,
                transitionRunning = common.getValue("transitionRunning") as Boolean,
                isCurrentMasterPlayer = common.getValue("isCurrentMasterPlayer") as Boolean,
                mediaIdMatches = common.getValue("mediaIdMatches") as Boolean,
                playbackState = common.getValue("playbackState") as Int,
                isPlaying = common.getValue("isPlaying") as Boolean,
                playWhenReady = common.getValue("playWhenReady") as Boolean,
                playbackSuppressionReason = common.getValue("playbackSuppressionReason") as Int
            )
        ).isTrue()

        assertThat(
            shouldTriggerAudioOffloadStallFallback(
                audioOffloadEnabled = true,
                transitionRunning = true,
                isCurrentMasterPlayer = true,
                mediaIdMatches = true,
                playbackState = Player.STATE_READY,
                isPlaying = false,
                playWhenReady = true,
                playbackSuppressionReason = Player.PLAYBACK_SUPPRESSION_REASON_NONE
            )
        ).isFalse()
    }
}
