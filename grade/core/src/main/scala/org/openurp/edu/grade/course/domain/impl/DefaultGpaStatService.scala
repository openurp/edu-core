package org.openurp.edu.grade.course.domain.impl

import java.util.Date

import org.openurp.base.model.Semester
import org.openurp.edu.base.model.{ Course, Student }
import org.openurp.edu.grade.course.domain.{ CourseGradeProvider, GpaPolicy, GpaStatService, MultiStdGpa }
import org.openurp.edu.grade.course.model.{ CourseGrade, StdGpa, StdSemesterGpa, StdYearGpa }

class DefaultGpaStatService extends GpaStatService {

  var courseGradeProvider: CourseGradeProvider = _

  var gpaPolicy: GpaPolicy = _

  def statGpa(std: Student, grades: Iterable[CourseGrade]): StdGpa = {
    val gradesMap = new collection.mutable.HashMap[Semester, collection.mutable.ListBuffer[CourseGrade]]
    val courseMap = new collection.mutable.HashMap[Course, CourseGrade]
    for (grade <- grades) {
      val semesterGrades = gradesMap.getOrElseUpdate(grade.semester, new collection.mutable.ListBuffer[CourseGrade])
      courseMap.get(grade.course) match {
        case Some(exist) => if (!exist.passed) courseMap.put(grade.course, grade)
        case None => courseMap.put(grade.course, grade)
      }
      semesterGrades += grade
    }
    val stdGpa = new StdGpa(std)
    val yearGradeMap = new collection.mutable.HashMap[String, collection.mutable.ListBuffer[CourseGrade]]
    for (semester <- gradesMap.keySet) {
      val stdTermGpa = new StdSemesterGpa()
      stdTermGpa.semester = semester
      stdGpa.add(stdTermGpa)
      val semesterGrades = gradesMap(semester)
      val yearGrades = yearGradeMap.getOrElseUpdate(semester.schoolYear, new collection.mutable.ListBuffer[CourseGrade])
      yearGrades ++= semesterGrades
      stdTermGpa.gpa = gpaPolicy.calcGpa(semesterGrades)
      stdTermGpa.ga = gpaPolicy.calcGa(semesterGrades)
      stdTermGpa.count = semesterGrades.size
      val stats = statCredits(semesterGrades)
      stdTermGpa.credits = stats(0)
      stdTermGpa.obtainedCredits = stats(1)
    }
    for (year <- yearGradeMap.keySet) {
      val stdYearGpa = new StdYearGpa()
      stdYearGpa.schoolYear = year
      stdGpa.add(stdYearGpa)
      val yearGrades = yearGradeMap(year)
      stdYearGpa.gpa = gpaPolicy.calcGpa(yearGrades)
      stdYearGpa.ga = gpaPolicy.calcGa(yearGrades)
      stdYearGpa.count = yearGrades.size
      val stats = statCredits(yearGrades)
      stdYearGpa.credits = stats(0)
      stdYearGpa.obtainedCredits = stats(1)
    }
    stdGpa.gpa = gpaPolicy.calcGpa(grades)
    stdGpa.ga = gpaPolicy.calcGa(grades)
    stdGpa.count = courseMap.size
    val totalStats = statCredits(courseMap.values)
    stdGpa.credits = totalStats(0)
    stdGpa.obtainedCredits = totalStats(1)
    val now = new Date()
    stdGpa.updatedAt = now
    stdGpa
  }

  def statGpa(std: Student, semesters: Semester*): StdGpa = {
    statGpa(std, courseGradeProvider.getPublished(std, semesters: _*))
  }

  def statGpas(stds: Iterable[Student], semesters: Semester*): MultiStdGpa = {
    val semesterGpas = new collection.mutable.ListBuffer[StdGpa]
    for (std <- stds) {
      semesterGpas += statGpa(std, semesters: _*)
    }
    new MultiStdGpa(stds, semesterGpas)
  }

  /**
   * ???????????????0??????????????????1???????????????
   */
  private def statCredits(grades: Iterable[CourseGrade]): Array[Float] = {
    var credits = 0f
    var all = 0f
    for (grade <- grades) {
      if (grade.passed) credits += grade.course.credits
      all += grade.course.credits
    }
    Array(all, credits)
  }
}
