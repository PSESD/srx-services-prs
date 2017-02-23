package org.psesd.srx.services.prs

import java.util.UUID

import org.psesd.srx.shared.core.SrxResourceType
import org.psesd.srx.shared.core.config.Environment
import org.psesd.srx.shared.core.sif._


/**
  * Created by Kristy Overton on 2/21/2017.
  */
object XsreRefreshRequestService {

  def sendRequest(zone: SifZone, studentId: String, generatorId: String) : SifResponse = {

      val sifRequest = new SifRequest(Environment.srxProvider, SrxResourceType.XsresRefresh.toString, zone, SifContext("DEFAULT")) {
        generatorId = generatorId
        queueId = Some(UUID.randomUUID().toString)
        requestId = Some(UUID.randomUUID().toString)
        requestType = Some(SifRequestType.Delayed)
        serviceType = Some(SifServiceType.Functional)
        body = Some("<request><studentIds><studentId>" + studentId + "</studentId></studentIds></request>")
      }

    try {
      new SifConsumer().create(sifRequest)
    }
    catch {
      case _ : Throwable =>
        new SifResponse(sifRequest.timestamp, sifRequest.messageId.getOrElse(new SifMessageId(UUID.randomUUID())), (sifRequest.messageType).get, sifRequest) {
          statusCode = 500
        }
    }
  }

}
