/*
 * Copyright (C) 2014, The OpenURP Software.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

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
