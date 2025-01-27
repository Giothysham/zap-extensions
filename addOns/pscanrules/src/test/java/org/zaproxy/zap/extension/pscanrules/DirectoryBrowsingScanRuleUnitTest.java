/*
 * Zed Attack Proxy (ZAP) and its related class files.
 *
 * ZAP is an HTTP/HTTPS proxy for assessing web application security.
 *
 * Copyright 2018 The ZAP Development Team
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
 */
package org.zaproxy.zap.extension.pscanrules;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import java.util.List;
import java.util.Map;
import org.apache.commons.httpclient.URI;
import org.apache.commons.httpclient.URIException;
import org.junit.jupiter.api.Test;
import org.parosproxy.paros.core.scanner.Alert;
import org.parosproxy.paros.network.HttpMessage;
import org.parosproxy.paros.network.HttpRequestHeader;
import org.parosproxy.paros.network.HttpResponseHeader;
import org.parosproxy.paros.network.HttpStatusCode;
import org.zaproxy.addon.commonlib.CommonAlertTag;

class DirectoryBrowsingScanRuleUnitTest extends PassiveScannerTest<DirectoryBrowsingScanRule> {

    private HttpMessage createMessage() throws URIException {
        HttpRequestHeader requestHeader = new HttpRequestHeader();
        requestHeader.setURI(new URI("http://example.com", false));

        HttpMessage msg = new HttpMessage();
        msg.setRequestHeader(requestHeader);
        msg.getResponseHeader().setStatusCode(HttpStatusCode.OK);
        msg.getResponseHeader().setHeader(HttpResponseHeader.CONTENT_TYPE, "text/html");
        return msg;
    }

    @Override
    protected DirectoryBrowsingScanRule createScanner() {
        return new DirectoryBrowsingScanRule();
    }

    @Test
    void shouldNotRaiseAlertIfResponseBodyIsEmpty() throws URIException {
        // Given
        HttpMessage msg = createMessage();
        // When
        scanHttpResponseReceive(msg);
        // Then
        assertThat(alertsRaised.size(), equalTo(0));
    }

    @Test
    void shouldNotRaiseAlertIfResponseBodyIsIrrelevant() throws URIException {
        // Given
        HttpMessage msg = createMessage();
        msg.setResponseBody("<html><H1>Some Title</H1></html>");
        // When
        scanHttpResponseReceive(msg);
        // Then
        assertThat(alertsRaised.size(), equalTo(0));
    }

    @Test
    void shouldRaiseAlertIfResponseContainsApacheStyleIndex() throws URIException {
        // Given
        HttpMessage msg = createMessage();
        msg.setResponseBody("<html><title>Index of /htdocs</title></html>");
        // When
        scanHttpResponseReceive(msg);
        // Then
        assertThat(alertsRaised.size(), equalTo(1));
    }

    @Test
    void shouldRaiseAlertIfResponseContainsIisStyleIndex() throws URIException {
        // Given
        HttpMessage msg = createMessage();
        msg.setResponseBody(
                "<html><head><title>somesite.net - /site/</title></head><body><H1>somesite.net - /site/</H1><hr><pre><A HREF=\"/\">[To Parent Directory]</A><br><br> 6/12/2014  3:25 AM        &lt;dir&gt; <A HREF=\"/site/file.ext/\">file.ext</A><br></pre><hr></body></html>");
        // When
        scanHttpResponseReceive(msg);
        // Then
        assertThat(alertsRaised.size(), equalTo(1));
    }

    @Test
    void shouldReturnExpectedMappings() {
        // Given / When
        Map<String, String> tags = rule.getAlertTags();
        // Then
        assertThat(tags.size(), is(equalTo(2)));
        assertThat(
                tags.containsKey(CommonAlertTag.OWASP_2021_A05_SEC_MISCONFIG.getTag()),
                is(equalTo(true)));
        assertThat(
                tags.containsKey(CommonAlertTag.OWASP_2017_A06_SEC_MISCONFIG.getTag()),
                is(equalTo(true)));
        assertThat(
                tags.get(CommonAlertTag.OWASP_2021_A05_SEC_MISCONFIG.getTag()),
                is(equalTo(CommonAlertTag.OWASP_2021_A05_SEC_MISCONFIG.getValue())));
        assertThat(
                tags.get(CommonAlertTag.OWASP_2017_A06_SEC_MISCONFIG.getTag()),
                is(equalTo(CommonAlertTag.OWASP_2017_A06_SEC_MISCONFIG.getValue())));
    }

    @Test
    void shouldReturnExpectedExampleAlert() {
        // Given / When
        List<Alert> alerts = rule.getExampleAlerts();
        // Then
        assertThat(alerts.size(), is(equalTo(2)));
        Alert alertApache = alerts.get(0);
        assertThat(alertApache.getRisk(), is(equalTo(Alert.RISK_MEDIUM)));
        assertThat(alertApache.getConfidence(), is(equalTo(Alert.CONFIDENCE_MEDIUM)));
        assertThat(alertApache.getName(), is(equalTo("Directory Browsing - Apache 2")));
        assertThat(
                alertApache.getEvidence(),
                is(equalTo("<html><title>Index of /htdocs</title></html>")));
        Alert alertMicrosoft = alerts.get(1);
        assertThat(alertMicrosoft.getRisk(), is(equalTo(Alert.RISK_MEDIUM)));
        assertThat(alertMicrosoft.getConfidence(), is(equalTo(Alert.CONFIDENCE_MEDIUM)));
        assertThat(
                alertMicrosoft.getEvidence(),
                is(equalTo("<pre><A HREF=\"/\">[To Parent Directory]</A><br><br>")));
        assertThat(alertMicrosoft.getName(), is(equalTo("Directory Browsing - Microsoft IIS")));
    }
}
