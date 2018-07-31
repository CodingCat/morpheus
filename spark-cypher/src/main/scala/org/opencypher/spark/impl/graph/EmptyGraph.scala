/*
 * Copyright (c) 2016-2018 "Neo4j Sweden, AB" [https://neo4j.com]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Attribution Notice under the terms of the Apache License 2.0
 *
 * This work was created by the collective efforts of the openCypher community.
 * Without limiting the terms of Section 6, any Derivative Work that is not
 * approved by the public consensus process of the openCypher Implementers Group
 * should not be described as “Cypher” (and Cypher® is a registered trademark of
 * Neo4j Inc.) or as "openCypher". Extensions by implementers or prototypes or
 * proposals for change that have been documented or implemented should only be
 * described as "implementation extensions to Cypher" or as "proposed changes to
 * Cypher that are not yet approved by the openCypher community".
 */
package org.opencypher.spark.impl.graph

import org.opencypher.okapi.api.types.CypherType
import org.opencypher.okapi.ir.api.expr.Var
import org.opencypher.okapi.relational.api.graph.RelationalCypherGraph
import org.opencypher.okapi.relational.impl.operators.{RelationalOperator, Start}
import org.opencypher.okapi.relational.impl.table.RecordHeader
import org.opencypher.spark.api.CAPSSession
import org.opencypher.spark.impl.CAPSRecords
import org.opencypher.spark.impl.table.SparkTable.DataFrameTable
import org.opencypher.spark.schema.CAPSSchema

sealed case class EmptyGraph(implicit val caps: CAPSSession) extends RelationalCypherGraph[DataFrameTable] {

  override type Session = CAPSSession

  override type Records = CAPSRecords

  override val schema: CAPSSchema = CAPSSchema.empty

  override def session: CAPSSession = caps

  override def cache(): EmptyGraph = this

  override def tables: Seq[DataFrameTable] = Seq.empty

  override def tags: Set[Int] = Set.empty

  override private[opencypher] def scanOperator(
    entityType: CypherType,
    exactLabelMatch: Boolean
  ): RelationalOperator[DataFrameTable] = {
    val scanHeader = RecordHeader.empty.withExpr(Var("")(entityType))
    Start(caps.records.empty(scanHeader))(session.basicRuntimeContext())
  }
}