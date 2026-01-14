package org.openurp.edu.grade.service.impl

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

class SettingTest extends AnyFunSpec, Matchers {

  describe("Filter") {
    it("parse json") {
      val json = """{"gaElementTypes":[{"id":2,"name":"期末成绩"},{"id":3,"name":"平时成绩"}],"noMakeupExamStatuses":[{"id":2,"name":"缓考"},{"id":3,"name":"缺考"}],"noMakeupTakeTypes":[{"name":"重修","id":3}],"emptyScoreStatuses":[{"id":2,"name":"缓考"},{"id":3,"name":"缺考"}],"submitIsPublish":false}"""
      val setting = CourseGradeSettingsImpl.parse(json)
      setting.gaElementTypes.size should be(2)
    }
  }

}
