package infcalcs.actors

import akka.actor.Actor
import infcalcs._
import infcalcs.exceptions.{InappropriateInitBinsException, ExcessActorException}

/**
 * Created by ryansuderman on 9/18/15.
 */

/**
 * Abstract class inherited by [[FixedDistributor]] and [[AdaptiveDistributor]]
 * that holds a number of methods and variables for managing a parallel
 * implementation of channel capacity estimation
 *
 * @param p
 * @param calcConfig
 */
abstract class Distributor(p: DRData)(implicit calcConfig: CalcConfig) extends Actor {

  /** Results sent back from [[Calculator]] instances */
  var estList: Array[EstTuple] = Array()

  /** Tracks the number of signal bins for calculations */
  var signalBins: NTuple[Int] = calcConfig.initSignalBins

  /** List of weights to try for a particular number of signal bins */
  var weights = EstimateCC.getWeights(calcConfig)(p, signalBins)

  var totalCalculations = weights.length

  var sent = 0

  var received = 0

  var sigIndex = 0

  def sentCalc() = sent = sent + 1

  def receivedCalc() = received = received + 1

  def sentAllCalcs: Boolean = sent == totalCalculations

  def receivedAllCalcs: Boolean = received == totalCalculations

  def updateEstList(r: Result) = estList = estList :+ r.res

  /**
   * Initializes some number of [[Calculator]] instances to calculate
   * mutual information estimates
   *
   * @param init
   */
  def initializeCalculators(init: Init) =
    if (!EstimateMI.binNumberIsAppropriate(calcConfig)(p, (calcConfig.initBinTuples)))
      throw new InappropriateInitBinsException("initial bin numbers are too large")
    else {
      if (init.numActors < weights.length) {
        val calcList = (0 until init.numActors).toList map (x =>
          context actorOf(Calculator props calcConfig, s"calc_${x}"))

        calcList foreach { c => {
          c ! Estimate(weights(sent), signalBins, p, sent, sigIndex)
          sentCalc()
        }
        }
      } else {
        // requires that the number of actors is less than the number of weights per signal bin number
        throw new ExcessActorException("excess actors")
      }
    }

  /**
   * Stops actor-based estimation of the channel capacity and outputs final result
   */
  def stopCalculation() = {
    if (EstCC.appConfig.verbose) {
      println(s"Stop criterion reached with ${signalBins.product} total bins")
    }
    val maxOpt = EstimateMI.optMIMult(calcConfig)(estList.toVector)
    EstimateMI.finalEstimation(
      maxOpt.pairBinTuples,
      p,
      maxOpt.weight)(calcConfig)
    println(s"${(maxOpt.estimates getOrElse Estimates((0.0, 0.0), Nil, 0.0)).dataEstimate._1}")
    context.system.shutdown()
  }

}
