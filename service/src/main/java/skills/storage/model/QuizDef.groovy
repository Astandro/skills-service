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
package skills.storage.model

import groovy.transform.CompileStatic
import groovy.transform.ToString
import org.springframework.data.jpa.domain.support.AuditingEntityListener

import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.Table

@Entity
@Table(name = 'quiz_definition')
@EntityListeners(AuditingEntityListener)
@CompileStatic
@ToString(includeNames = true)
class QuizDef extends QuizDefParent {

}
