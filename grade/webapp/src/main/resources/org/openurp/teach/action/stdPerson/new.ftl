[#ftl]
[@b.head/]
[@b.toolbar title="新建学生基本信息"]bar.addBack();[/@]
[@b.tabs]
  [@b.form action="!save" theme="list"]
    [@b.textfield name="stdPerson.code" label="人员编码" value="${stdPerson.code!}" required="true" maxlength="20"/]
    [@b.textfield name="stdPerson.name" label="姓名" value="${stdPerson.name!}" required="true" maxlength="20"/]
    [@b.textfield name="stdPerson.engName" label="英文名" value="${stdPerson.engName!}" maxlength="100"/]
    [@b.textfield name="stdPerson.oldname" label="曾用名" value="${stdPerson.oldname!}" maxlength="100"/]
    [@b.select name="stdPerson.gender.id" label="性别" value="${(stdPerson.gender.id)!}" required="true"
    	         style="width:200px;" items=genders option="id,name" empty="..."/]
    [@b.textfield label="出生日期" name="stdPerson.birthday" value="${stdPerson.birthday!}" required="true"/]
    [@b.textfield name="stdPerson.idcard" label="身份证" value="${stdPerson.idcard!}" required="true" /]
    [@b.textfield name="stdPerson.joinOn" label="入团(党)时间" value="${stdPerson.joinOn!}" /]
    [@b.textfield name="stdPerson.ancestralAddr" label="籍贯" value="${stdPerson.ancestralAddr!}" /]
    [@b.textfield name="stdPerson.charactor" label="特长爱好以及个人说明" value="${stdPerson.charactor!}" /]
    [@b.select name="stdPerson.idType.id" label="证件类型" value="${(stdPerson.idType.id)!}" required="true" 
               style="width:200px;" items=idTypes option="id,name" empty="..."/]
    [@b.select name="stdPerson.country.id" label="国家地区" value="${(stdPerson.country.id)!}" required="true" 
               style="width:200px;" items=countries option="id,name" empty="..."/]
    [@b.select name="stdPerson.nation.id" label="民族" value="${(stdPerson.nation.id)!}" required="true" 
               style="width:200px;" items=nations option="id,name" empty="..."/]
    [@b.select name="stdPerson.politicalAffiliation.id" label="政治面貌" value="${(stdPerson.politicalAffiliation.id)!}" required="true" 
               style="width:200px;" items=politicalAffiliations option="id,name" empty="..."/]
               
    [@b.formfoot]
      [@b.reset/]&nbsp;&nbsp;[@b.submit value="action.submit"/]
    [/@]
  [/@]
[/@]
[@b.foot/]