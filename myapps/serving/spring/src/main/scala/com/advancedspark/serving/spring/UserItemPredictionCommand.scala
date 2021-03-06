package com.advancedspark.serving.spring

import com.netflix.hystrix.HystrixCommand
import com.netflix.hystrix.HystrixCommandGroupKey

import org.jblas.DoubleMatrix

import scala.util.parsing.json._

class UserItemPredictionCommand(userId: Int, itemId: Int) 
    extends HystrixCommand[Double](HystrixCommandGroupKey.Factory.asKey("UserItemPredictionCommand")) {

  @throws(classOf[java.io.IOException])
  def get(url: String) = io.Source.fromURL(url).mkString

  def run(): Double = {
    val userFactorsStr = get(s"""http://127.0.0.1:9200/advancedspark/user-factors-als/_search?q=userId:${userId}""")

    // This is the worst piece of code I've ever written
    val userFactorsJson = JSON.parseFull(userFactorsStr)
    val userFactors = userFactorsJson.get
      .asInstanceOf[Map[String,Any]]("hits")
      .asInstanceOf[Map[String,Any]]("hits")
      .asInstanceOf[List[Any]](0)
      .asInstanceOf[Map[String,Any]]("_source")
      .asInstanceOf[Map[String,Any]]("userFactors")
      .asInstanceOf[List[Double]]

    // This is the second worst piece of code I've ever written
    val itemFactorsStr = get(s"""http://127.0.0.1:9200/advancedspark/item-factors-als/_search?q=itemId:${itemId}""")   
    val itemFactorsJson = JSON.parseFull(itemFactorsStr)
    val itemFactors = itemFactorsJson.get
      .asInstanceOf[Map[String,Any]]("hits")
      .asInstanceOf[Map[String,Any]]("hits")
      .asInstanceOf[List[Any]](0)
      .asInstanceOf[Map[String,Any]]("_source")
      .asInstanceOf[Map[String,Any]]("itemFactors")
      .asInstanceOf[List[Double]]
 
    try{
      val userFactorsMatrix = new DoubleMatrix(userFactors.toArray)
      val itemFactorsMatrix = new DoubleMatrix(itemFactors.toArray)
     
      // Calculate prediction 
      userFactorsMatrix.dot(itemFactorsMatrix)
    } catch { 
       case e: Throwable => {
         System.out.println(e) 
         throw e
       }
    }

   // 1.0;
  }

  override def getFallback(): Double = {
    // Retrieve fallback (ie. non-personalized top k)
    //val source = scala.io.Source.fromFile("/root/pipeline/datasets/serving/recommendations/fallback/model.json")
    //val fallbackRecommendationsModel = try source.mkString finally source.close()
    //return fallbackRecommendationsModel;

    System.out.println("UserItemPrediction Source is Down!  Fallback!!")

    0.0;
  }
}
