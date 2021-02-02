/**
 * Copyright 2020 SkillTree
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package skills.intTests.clientDisplay

import groovy.time.TimeCategory
import org.joda.time.format.DateTimeFormat
import org.joda.time.format.DateTimeFormatter
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsFactory
import skills.storage.model.SkillDef
import spock.lang.IgnoreRest

class SingleSkillSummarySpec extends DefaultIntSpec {

    def "load single skill summary"() {
        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj = SkillsFactory.createSubject(1, 1)
        List<Map> proj1_skills = SkillsFactory.createSkills(3, 1, 1)

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj)
        skillsService.createSkills(proj1_skills)

        when:
        def summary = skillsService.getSingleSkillSummary("user1", proj1.projectId, proj1_skills.get(1).skillId)

        then:
        !summary.crossProject
        summary.projectId == proj1.projectId
        summary.projectName == proj1.name
        summary.skillId == proj1_skills.get(1).skillId
        summary.skill == proj1_skills.get(1).name
        summary.pointIncrement == proj1_skills.get(1).pointIncrement
        summary.maxOccurrencesWithinIncrementInterval == proj1_skills.get(1).numMaxOccurrencesIncrementInterval
        summary.totalPoints == proj1_skills.get(1).pointIncrement * proj1_skills.get(1).numPerformToCompletion
        summary.points == 0
        summary.todaysPoints == 0
        summary.description.description == "This skill [skill2] belongs to project [TestProject1]"
    }

    def "load single skill summary with some users points"() {
        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj = SkillsFactory.createSubject(1, 1)
        List<Map> proj1_skills = SkillsFactory.createSkills(3, 1, 1)
        proj1_skills.each {
            it.numPerformToCompletion = 5
        }

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj)
        skillsService.createSkills(proj1_skills)

        String userId = "user1"
        Date yesterday = use(TimeCategory) { return 1.day.ago }
        when:
        skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(1).skillId], userId, new Date())
        skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(1).skillId], userId, yesterday)
        def summary = skillsService.getSingleSkillSummary(userId, proj1.projectId, proj1_skills.get(1).skillId)

        then:
        !summary.crossProject
        summary.projectId == proj1.projectId
        summary.projectName == proj1.name
        summary.skillId == proj1_skills.get(1).skillId
        summary.skill == proj1_skills.get(1).name
        summary.pointIncrement == proj1_skills.get(1).pointIncrement
        summary.totalPoints == proj1_skills.get(1).pointIncrement * proj1_skills.get(1).numPerformToCompletion
        summary.points == 20
        summary.todaysPoints == 10
        summary.description.description == "This skill [skill2] belongs to project [TestProject1]"
        !summary.dependencyInfo
    }


    def "load single skill summary with dependencies"() {
        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj = SkillsFactory.createSubject(1, 1)
        List<Map> proj1_skills = SkillsFactory.createSkills(3, 1, 1)
        proj1_skills.each {
            it.numPerformToCompletion = 5
        }

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj)
        skillsService.createSkills(proj1_skills)

        String userId = "user1"
        when:
        skillsService.assignDependency([projectId: proj1.projectId, skillId: proj1_skills.get(1).skillId, dependentSkillId: proj1_skills.get(0).skillId])
        skillsService.assignDependency([projectId: proj1.projectId, skillId: proj1_skills.get(1).skillId, dependentSkillId: proj1_skills.get(2).skillId])
        def summary = skillsService.getSingleSkillSummary(userId, proj1.projectId, proj1_skills.get(1).skillId)

        then:
        summary.projectId == proj1.projectId
        summary.skillId == proj1_skills.get(1).skillId
        summary.dependencyInfo.numDirectDependents == 2
        !summary.dependencyInfo.achieved
    }

    def "load single skill summary with all dependencies achieved"() {
        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj = SkillsFactory.createSubject(1, 1)
        List<Map> proj1_skills = SkillsFactory.createSkills(3, 1, 1)
        proj1_skills.each{
            it.pointIncrement = 40
        }

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj)
        skillsService.createSkills(proj1_skills)

        String userId = "user1"
        when:
        skillsService.assignDependency([projectId: proj1.projectId, skillId: proj1_skills.get(1).skillId, dependentSkillId: proj1_skills.get(0).skillId])
        skillsService.assignDependency([projectId: proj1.projectId, skillId: proj1_skills.get(1).skillId, dependentSkillId: proj1_skills.get(2).skillId])
        skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(0).skillId], userId, new Date())
        skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(2).skillId], userId, new Date())

        def summary = skillsService.getSingleSkillSummary(userId, proj1.projectId, proj1_skills.get(1).skillId)

        then:
        summary.projectId == proj1.projectId
        summary.skillId == proj1_skills.get(1).skillId
        summary.dependencyInfo.numDirectDependents == 2
        summary.dependencyInfo.achieved
    }

    def "if [help.url.root] property was set then must be used as a root for help url"() {
        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj = SkillsFactory.createSubject(1, 1)
        List<Map> proj1_skills = SkillsFactory.createSkills(3, 1, 1)
        proj1_skills.get(0).helpUrl = "/some/path"
        proj1_skills.get(1).helpUrl = "https://keepMe.com/some/path"

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj)
        skillsService.createSkills(proj1_skills)

        skillsService.changeSetting(proj1.projectId, "help.url.root", [projectId: proj1.projectId, setting: "help.url.root", value: "http://www.root.com/"])

        when:
        def summary = skillsService.getSingleSkillSummary("user1", proj1.projectId, proj1_skills.get(0).skillId)
        def summary1 = skillsService.getSingleSkillSummary("user1", proj1.projectId, proj1_skills.get(1).skillId)

        then:
        summary.description.href == "http://www.root.com/some/path"
        summary1.description.href == "https://keepMe.com/some/path"
    }

    DateTimeFormatter formatter = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
    def "achieved date"() {
        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj = SkillsFactory.createSubject(1, 1)
        List<Map> proj1_skills = SkillsFactory.createSkills(3, 1, 1)
        proj1_skills[1].numPerformToCompletion = 2
        proj1_skills.each {
            it.pointIncrement = 100
        }

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj)
        skillsService.createSkills(proj1_skills)

        Date date = new Date()
        String userId = "user1"
        when:
        skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(0).skillId], userId, date)
        skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(1).skillId], userId, date)

        def summary = skillsService.getSingleSkillSummary(userId, proj1.projectId, proj1_skills.get(0).skillId)
        def summary1 = skillsService.getSingleSkillSummary(userId, proj1.projectId, proj1_skills.get(1).skillId)
        def summary2 = skillsService.getSingleSkillSummary(userId, proj1.projectId, proj1_skills.get(2).skillId)

        then:
        formatter.parseDateTime(summary.achievedOn).getMillis() == date.time
        !summary1.achievedOn
        !summary2.achievedOn
    }

    def "load with different self-reporting types and status"() {
        def proj1 = SkillsFactory.createProject(1)
        def subj = SkillsFactory.createSubject(1, 1)
        List<Map> skills = SkillsFactory.createSkills(5, 1, 1, 100)
        skills[0].selfReportType = SkillDef.SelfReportingType.Approval
        skills[1].selfReportType = SkillDef.SelfReportingType.HonorSystem
        skills[2].selfReportType = SkillDef.SelfReportingType.Approval
        skills[3].selfReportType = SkillDef.SelfReportingType.Approval

        skillsService.createProject(proj1)
        skillsService.createSubject(subj)
        skillsService.createSkills(skills)

        String userId = "user0"
        Date date = new Date()
        skillsService.addSkill([projectId: proj1.projectId, skillId: skills.get(1).skillId], userId, date)
        skillsService.addSkill([projectId: proj1.projectId, skillId: skills.get(2).skillId], userId, date)
        skillsService.addSkill([projectId: proj1.projectId, skillId: skills.get(3).skillId], userId, date)
        Integer approvalIdToReject = skillsService.getApprovals(proj1.projectId).find { it.skillId == skills.get(3).skillId }.id
        skillsService.rejectSkillApprovals(proj1.projectId, [approvalIdToReject], 'Good rejection message')

        when:
        def summary1 = skillsService.getSingleSkillSummary(userId, proj1.projectId, skills.get(0).skillId)
        def summary2 = skillsService.getSingleSkillSummary(userId, proj1.projectId, skills.get(1).skillId)
        def summary3 = skillsService.getSingleSkillSummary(userId, proj1.projectId, skills.get(2).skillId)
        def summary4 = skillsService.getSingleSkillSummary(userId, proj1.projectId, skills.get(3).skillId)
        def summary5 = skillsService.getSingleSkillSummary(userId, proj1.projectId, skills.get(4).skillId)

        then:
        summary1.selfReporting.enabled
        summary1.selfReporting.type == SkillDef.SelfReportingType.Approval.toString()
        !summary1.selfReporting.requestedOn
        !summary1.selfReporting.rejectedOn
        !summary1.selfReporting.rejectionMsg

        summary2.selfReporting.enabled
        summary2.selfReporting.type == SkillDef.SelfReportingType.HonorSystem.toString()
        !summary2.selfReporting.requestedOn
        !summary2.selfReporting.rejectedOn
        !summary2.selfReporting.rejectionMsg

        summary3.selfReporting.enabled
        summary3.selfReporting.type == SkillDef.SelfReportingType.Approval.toString()
        summary3.selfReporting.requestedOn == date.time
        !summary3.selfReporting.rejectedOn
        !summary3.selfReporting.rejectionMsg

        summary4.selfReporting.enabled
        summary4.selfReporting.type == SkillDef.SelfReportingType.Approval.toString()
        summary4.selfReporting.requestedOn == date.time
        new Date(summary4.selfReporting.rejectedOn).format('yyyy-MM-dd') == date.format('yyyy-MM-dd')
        summary4.selfReporting.rejectionMsg == 'Good rejection message'

        !summary5.selfReporting.enabled
        !summary5.selfReporting.type
        !summary5.selfReporting.requestedOn
        !summary5.selfReporting.rejectedOn
        !summary5.selfReporting.rejectionMsg
    }



}
