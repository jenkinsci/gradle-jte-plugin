package org.jenkinsci.gradle.plugins.jte

import org.junit.ClassRule
import org.jvnet.hudson.test.JenkinsRule
import spock.lang.Shared
import spock.lang.Specification

class TestSpec extends Specification {

    @Shared @ClassRule JenkinsRule jenkins = new JenkinsRule()

    def "just testing"(){
        given:
        println jenkins.getInstance()
        expect:
        assert true
    }

}
