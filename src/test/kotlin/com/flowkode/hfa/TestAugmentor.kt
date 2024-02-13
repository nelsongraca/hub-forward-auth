package com.flowkode.hfa

import io.quarkus.security.identity.SecurityIdentity
import io.quarkus.security.runtime.QuarkusSecurityIdentity
import io.quarkus.test.security.TestSecurity
import io.quarkus.test.security.TestSecurityIdentityAugmentor
import jakarta.enterprise.context.ApplicationScoped

@ApplicationScoped
class TestAugmentor : TestSecurityIdentityAugmentor {
    override fun augment(identity: SecurityIdentity, annotations: Array<Annotation>): SecurityIdentity {
        if (identity.isAnonymous) {
            return identity
        }
        val urls = annotations
            .asSequence()
            .filterIsInstance<TestSecurity>()
            .flatMap { it.attributes.toSet() }
            .filter { it.key == "urls" }
            .map { it.value }
            .toSet()
        val builder = QuarkusSecurityIdentity.builder(identity)
        if (urls.isNotEmpty()) {
            builder.addAttribute(HubAugmentor.SERVICE_URLS, urls)
        }
        return builder.build()
    }
}