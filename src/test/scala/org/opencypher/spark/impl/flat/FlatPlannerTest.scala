package org.opencypher.spark.impl.flat

import org.opencypher.spark.StdTestSuite
import org.opencypher.spark.api.types._
import org.opencypher.spark.api.expr.{HasLabel, Property, TrueLit, Var}
import org.opencypher.spark.api.ir.Field
import org.opencypher.spark.api.ir.global.GlobalsRegistry
import org.opencypher.spark.api.ir.pattern.{AllOf, EveryNode}
import org.opencypher.spark.api.record.{OpaqueField, ProjectedExpr, ProjectedField, RecordSlot}
import org.opencypher.spark.api.schema.Schema
import org.opencypher.spark.impl.logical.LogicalOperatorProducer

class FlatPlannerTest extends StdTestSuite {

  val schema = Schema
    .empty
    .withNodeKeys("Person")("name" -> CTString, "age" -> CTInteger.nullable)
    .withNodeKeys("Employee")("name" -> CTString, "salary" -> CTFloat)

  val globals = GlobalsRegistry.fromSchema(schema)

  implicit val context = FlatPlannerContext(schema, globals)

  import globals._

  val mkLogical = new LogicalOperatorProducer
  val mkFlat = new FlatOperatorProducer()
  val flatPlanner = new FlatPlanner

  // TODO: Ids missing
  // TODO: Do not name schema provided columns

  test("Construct node scan") {
    val result = flatPlanner.process(mkLogical.planNodeScan(Field("n")(CTNode), EveryNode(AllOf(label("Person")))))
    val headerContents = result.header.contents

    val nodeVar = Var("n")(CTNode)

    result should equal(mkFlat.nodeScan(nodeVar, EveryNode(AllOf(label("Person")))))
    headerContents should equal(Set(
      OpaqueField(nodeVar),
      ProjectedExpr(HasLabel(nodeVar, label("Person"))(CTBoolean)),
      ProjectedExpr(Property(nodeVar, propertyKey("name"))(CTString)),
      ProjectedExpr(Property(nodeVar, propertyKey("age"))(CTInteger.nullable))
    ))
  }

  test("Construct unlabeled node scan") {
    val result = flatPlanner.process(mkLogical.planNodeScan(Field("n")(CTNode), EveryNode))
    val headerContents = result.header.contents

    val nodeVar = Var("n")(CTNode)

    result should equal(mkFlat.nodeScan(nodeVar, EveryNode))
    headerContents should equal(Set(
      OpaqueField(nodeVar),
      ProjectedExpr(HasLabel(nodeVar, label("Person"))(CTBoolean)),
      ProjectedExpr(HasLabel(nodeVar, label("Employee"))(CTBoolean)),
      ProjectedExpr(Property(nodeVar, propertyKey("name"))(CTString)),
      ProjectedExpr(Property(nodeVar, propertyKey("age"))(CTInteger.nullable)),
      ProjectedExpr(Property(nodeVar, propertyKey("salary"))(CTFloat.nullable))
    ))
  }

  test("Construct filtered node scan") {
    val result = flatPlanner.process(
      mkLogical.planFilter(TrueLit(),
        mkLogical.planNodeScan(Field("n")(CTNode), EveryNode)
      )
    )
    val headerContents = result.header.contents

    val nodeVar = Var("n")(CTNode)

    result should equal(
      mkFlat.filter(
        TrueLit(),
        mkFlat.nodeScan(nodeVar, EveryNode)
      )
    )
    headerContents should equal(Set(
      OpaqueField(nodeVar),
      ProjectedExpr(HasLabel(nodeVar, label("Person"))(CTBoolean)),
      ProjectedExpr(HasLabel(nodeVar, label("Employee"))(CTBoolean)),
      ProjectedExpr(Property(nodeVar, propertyKey("name"))(CTString)),
      ProjectedExpr(Property(nodeVar, propertyKey("age"))(CTInteger.nullable)),
      ProjectedExpr(Property(nodeVar, propertyKey("salary"))(CTFloat.nullable))
    ))
  }

  test("Construct selection") {
    val result = flatPlanner.process(
      mkLogical.planSelect(Set(Var("foo")(CTString)),
        mkLogical.projectField(Field("foo")(CTString), Property(Var("n")(CTNode), propertyKey("name"))(CTString),
          mkLogical.planNodeScan(Field("n")(CTNode), EveryNode(AllOf(label("Person"))))
        )
      )
    )
    val headerContents = result.header.contents

    result should equal(
      mkFlat.select(
        Set(Var("foo")(CTString)),
        mkFlat.project(
          ProjectedField(Var("foo")(CTString), Property(Var("n")(CTNode), propertyKey("name"))(CTString)),
          mkFlat.nodeScan(
            Var("n")(CTNode), EveryNode(AllOf(label("Person")))
          )
        )
      )
    )
    headerContents should equal(Set(
      ProjectedField(Var("foo")(CTString), Property(Var("n")(CTNode), propertyKey("name"))(CTString))
    ))
  }
}
