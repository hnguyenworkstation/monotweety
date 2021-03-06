package net.yslibrary.monotweety.status.domain

import com.twitter.twittertext.TwitterTextParser
import io.reactivex.Single
import net.yslibrary.monotweety.base.di.UserScope
import net.yslibrary.monotweety.data.config.ConfigDataManager
import javax.inject.Inject

@UserScope
class CheckStatusLength @Inject constructor(private val configDataManager: ConfigDataManager) {

  fun execute(status: String): Single<Result> {
    return configDataManager.shortUrlLengthHttps()
        .firstOrError()
        .map {
          val result = TwitterTextParser.parseTweet(status)
          Result(
              status = status,
              length = result.weightedLength,
              valid = result.isValid,
              maxLength = TwitterTextParser.TWITTER_TEXT_WEIGHTED_CHAR_COUNT_CONFIG.maxWeightedTweetLength)
        }

  }

  data class Result(val status: String,
                    val length: Int,
                    val valid: Boolean,
                    val maxLength: Int)
}