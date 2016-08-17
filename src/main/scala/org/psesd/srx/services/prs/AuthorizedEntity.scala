package org.psesd.srx.services.prs

import org.psesd.srx.shared.core.SrxResource
import org.psesd.srx.shared.core.exceptions.{ArgumentInvalidException, ArgumentNullException}
import org.psesd.srx.shared.core.extensions.TypeExtensions._
import org.psesd.srx.shared.data.{Datasource, DatasourceConfig, DatasourceResult}

import scala.xml.Node

/** Represents an Authorized Entity.
  *
  * @version 1.0
  * @since 1.0
  * @author Stephen Pugmire (iTrellis, LLC)
  */
class AuthorizedEntity(
                        val id: Int,
                        val name: String,
                        val mainContactId: Option[Int]
                      ) extends SrxResource {

  def toXml: Node = {
    <authorizedEntity>
      <id>{id.toString}</id>
      <name>{name}</name>
      <mainContactId>{mainContactId.getOrElse("").toString}</mainContactId>
    </authorizedEntity>
  }

}

object AuthorizedEntity extends PrsEntity {
  def apply(id: Int, name: String, mainContactId: Option[Int]): AuthorizedEntity = new AuthorizedEntity(id, name, mainContactId)

  def apply(authorizedEntityXml: Node): AuthorizedEntity = {
    if (authorizedEntityXml == null) {
      throw new ArgumentNullException("authorizedEntityXml parameter")
    }
    val rootElementName = authorizedEntityXml.label
    if (rootElementName != "authorizedEntity") {
      throw new ArgumentInvalidException("root element '%s'".format(rootElementName))
    }
    val id = (authorizedEntityXml \ "id").textOption.getOrElse("0").toInt
    val name = (authorizedEntityXml \ "name").textRequired("authorizedEntity.name")
    val mainContactId = (authorizedEntityXml \ "mainContactId").textOption.getOrElse("0").toInt
    new AuthorizedEntity(
      id,
      name, {
        if (mainContactId > 0) {
          Some(mainContactId)
        } else {
          None
        }
      }
    )
  }

  def create(authorizedEntity: AuthorizedEntity, datasourceConfig: DatasourceConfig): DatasourceResult = {
    if (authorizedEntity == null) {
      throw new ArgumentNullException("authorizedEntity parameter")
    }

    val datasource = new Datasource(datasourceConfig)

    val result = datasource.execute(
      "insert into srx_services_prs.authorized_entity (" +
        "id, name, main_contact_id) values (" +
        "DEFAULT, ?, ?) " +
        "RETURNING id;",
      authorizedEntity.name,
      authorizedEntity.mainContactId.orNull
    )

    datasource.close()

    result
  }
}
