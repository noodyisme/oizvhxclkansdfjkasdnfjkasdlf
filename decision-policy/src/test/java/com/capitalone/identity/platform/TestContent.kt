package com.capitalone.identity.platform.loading

class TestContent {

        companion object {

                @JvmField
                var DMN_BLANK: String = """
            <dmn:definitions xmlns:dmn="http://www.omg.org/spec/DMN/20180521/MODEL/" xmlns="https://kiegroup.org/dmn/_63BB0E02-4744-4897-ABBF-8A7AC0CFDCE9" xmlns:feel="http://www.omg.org/spec/DMN/20180521/FEEL/" xmlns:kie="http://www.drools.org/kie/dmn/1.2" xmlns:dmndi="http://www.omg.org/spec/DMN/20180521/DMNDI/" xmlns:di="http://www.omg.org/spec/DMN/20180521/DI/" xmlns:dc="http://www.omg.org/spec/DMN/20180521/DC/" id="_014BC6A5-D297-4E19-BF6A-356AE01CA923" name="unsaved file" typeLanguage="http://www.omg.org/spec/DMN/20180521/FEEL/" namespace="https://kiegroup.org/dmn/_63BB0E02-4744-4897-ABBF-8A7AC0CFDCE9">
                <dmn:extensionElements/>
                <dmndi:DMNDI>
                    <dmndi:DMNDiagram>
                        <di:extension>
                        <kie:ComponentsWidthsExtension/>
                        </di:extension>
                    </dmndi:DMNDiagram>
                </dmndi:DMNDI>
            </dmn:definitions> 
            """

                @JvmField
                var POLICY_METADATA_DECISION_AVAILABLE: String = """{"Type": "DECISION", "Status": "AVAILABLE"}"""

                @JvmField
                var POLICY_METADATA_MALFORMED: String = """{<testMalformedMedatata>}"""

                @JvmField
                var POLICY_METADATA: String = """"{"Status": "ACTIVE"}"""

                @JvmField
                var POLICY_METADATA_CONFIG_AVAILABLE: String = """
            {
              "Type": "CONFIG",
              "Status": "AVAILABLE"
            }
            """

                @JvmField
                var SAMPLE_PIP: String = """
            <routes xmlns="http://camel.apache.org/schema/spring">
                <route id="direct:sample-1.0-normal">
                    <from uri="direct:sample-1.0-normal"/>
                    <to uri="dx:sample:{{env.mimeoURL}}/api?method=get&amp;dxVersion=1"/>
                </route>
            </routes> 
        """

                @JvmField
                var SIMPLE_CONFIG_SCHEMA: String = """
        {
            "${"$"}schema": "https://json-schema.org/draft/2019-09/schema",
            "type": "object",
            "required": [ "param-A", "param-B" ],
            "additionalProperties": false,
            "properties": {
                "param-A": { "type": "string" },
                "param-B": { "type": "string" }
            }
        }
        """

                @JvmField
                var SIMPLE_CONFIG_DEFAULTS: String = """{"param-A":  "Z", "param-B":  "Y"}"""

                @JvmField
                var SIMPLE_CONFIG_USECASE_A: String = """{"param-A":  "A"}"""

                @JvmField
                var SIMPLE_CONFIG_V2_SCHEMA: String = """
        {
            "${"$"}id": "v2",
            "${"$"}defs": {
                "usecase": {
                    "${"$"}schema": "https://json-schema.org/draft/2019-09/schema",
                    "type": "object",
                    "additionalProperties": false,
                    "properties": {
                        "param-A": { "type": "string" }
                    }
            },
            "defaults": {
                "allOf": [
                    { "${"$"}ref": "usecase" },
                    { "required": [ "param-A" ] }
              ]
            },
            "features": {
              "${"$"}schema": "https://json-schema.org/draft/2019-09/schema",
              "type": "object",
              "additionalProperties": false,
              "properties": {
                  "param-B": { "type": "string" }
              }
            },
            "features-required": {
              "allOf": [
                { "${"$"}ref": "features" },
                { "required": ["param-B"] }
              ]
            }
          }
        }
        """

                @JvmField
                var SIMPLE_CONFIG_V2_DEFAULTS: String = """{"param-A":  "Z"}"""

                @JvmField
                var SIMPLE_CONFIG_V2_FEATURES: String = """{"param-B":  "Y"}"""

                @JvmField
                var SIMPLE_CONFIG_V2_FEATURES_QA: String = """{"param-B":  "Y-QA"}"""

                /**
                 * A dmn file with the following:
                 * Dmn Inputs:
                 * <ul>
                 *     <li>config.param-A</li>
                 *     <li>param-A</li>
                 * </ul>
                 * Dmn Outputs
                 * <ul>
                 *     <li>key: Decision-1 value: "Decision-1 Output [config.param-a=<config.param-A>, param-a=<param-A>]"</li>
                 *     <li>key: Decision-2 value: "Decision-2 Output [param-a=<param-A>]"</li>
                 * <ul>
                 */
                @JvmField
                var SIMPLE_DMN: String = """
             <dmn:definitions xmlns:dmn="http://www.omg.org/spec/DMN/20180521/MODEL/" xmlns="https://kiegroup.org/dmn/_1EA4B212-18E6-45E1-B8A9-F9D13B740C78" xmlns:feel="http://www.omg.org/spec/DMN/20180521/FEEL/" xmlns:kie="http://www.drools.org/kie/dmn/1.2" xmlns:dmndi="http://www.omg.org/spec/DMN/20180521/DMNDI/" xmlns:di="http://www.omg.org/spec/DMN/20180521/DI/" xmlns:dc="http://www.omg.org/spec/DMN/20180521/DC/" id="_F5AAEAC2-7ACE-4454-9037-5895A5FFB477" name="always-challenge-rule" typeLanguage="http://www.omg.org/spec/DMN/20180521/FEEL/" namespace="https://kiegroup.org/dmn/_1EA4B212-18E6-45E1-B8A9-F9D13B740C78">
              <dmn:extensionElements/>
              <dmn:inputData id="_F0F0BC0B-2330-4106-BE3D-76BAC7E76A47" name="param-A">
                <dmn:extensionElements/>
                <dmn:variable id="_CE1DC98A-E41E-4712-ACFF-54AED2D125EF" name="param-A"/>
              </dmn:inputData>
              <dmn:decision id="_E9C27301-0B5D-47CB-9964-6A131BEB7F9A" name="Decision-1">
                <dmn:extensionElements/>
                <dmn:variable id="_864A4E77-D071-4A3B-A082-774C43872B59" name="Decision-1" typeRef="string"/>
                <dmn:informationRequirement id="_D783B9FF-7D94-4EF1-8E76-6EA33D63BFDB">
                  <dmn:requiredInput href="#_F0F0BC0B-2330-4106-BE3D-76BAC7E76A47"/>
                </dmn:informationRequirement>
                <dmn:informationRequirement id="_C32FBEB9-B062-4B4A-BCAB-5D1A2BCDF9E4">
                  <dmn:requiredInput href="#_82350973-C1B3-4748-B9D0-3D6EB7A6974E"/>
                </dmn:informationRequirement>
                <dmn:literalExpression id="_E176B321-21DC-4011-838B-41D2AE050ACF">
                  <dmn:text>"Decision-1 Output (config.param-A=" + config.param-A + ", param-A=" + param-A + ")"</dmn:text>
                </dmn:literalExpression>
              </dmn:decision>
              <dmn:inputData id="_82350973-C1B3-4748-B9D0-3D6EB7A6974E" name="config.param-A">
                <dmn:extensionElements/>
                <dmn:variable id="_9104F129-D710-4323-850E-DB50E324F236" name="config.param-A"/>
              </dmn:inputData>
              <dmn:decision id="_327E5C1D-CED1-4090-BB57-9459C2CC91E7" name="Decision-2">
                <dmn:extensionElements/>
                <dmn:variable id="_BE1BE382-5048-4199-9C4F-574D67BBF440" name="Decision-2"/>
                <dmn:informationRequirement id="_D0B6E7F2-D257-4173-B6EA-028DF1F291D6">
                  <dmn:requiredInput href="#_F0F0BC0B-2330-4106-BE3D-76BAC7E76A47"/>
                </dmn:informationRequirement>
                <dmn:literalExpression id="_3E7D813F-7ED2-417C-8155-6A995BF737AB">
                  <dmn:text>"Decision-2 Output (param-A=" + param-A +")"</dmn:text>
                </dmn:literalExpression>
              </dmn:decision>
              <dmndi:DMNDI>
                <dmndi:DMNDiagram id="_E440D2F6-7BAB-41BB-AD03-09B625E228DC" name="DRG">
                  <di:extension>
                    <kie:ComponentsWidthsExtension>
                      <kie:ComponentWidths dmnElementRef="_E176B321-21DC-4011-838B-41D2AE050ACF">
                        <kie:width>300</kie:width>
                      </kie:ComponentWidths>
                      <kie:ComponentWidths dmnElementRef="_3E7D813F-7ED2-417C-8155-6A995BF737AB">
                        <kie:width>300</kie:width>
                      </kie:ComponentWidths>
                    </kie:ComponentsWidthsExtension>
                  </di:extension>
                  <dmndi:DMNShape id="dmnshape-drg-_F0F0BC0B-2330-4106-BE3D-76BAC7E76A47" dmnElementRef="_F0F0BC0B-2330-4106-BE3D-76BAC7E76A47" isCollapsed="false">
                    <dmndi:DMNStyle>
                      <dmndi:FillColor red="255" green="255" blue="255"/>
                      <dmndi:StrokeColor red="0" green="0" blue="0"/>
                      <dmndi:FontColor red="0" green="0" blue="0"/>
                    </dmndi:DMNStyle>
                    <dc:Bounds x="825" y="408" width="144" height="50"/>
                    <dmndi:DMNLabel/>
                  </dmndi:DMNShape>
                  <dmndi:DMNShape id="dmnshape-drg-_E9C27301-0B5D-47CB-9964-6A131BEB7F9A" dmnElementRef="_E9C27301-0B5D-47CB-9964-6A131BEB7F9A" isCollapsed="false">
                    <dmndi:DMNStyle>
                      <dmndi:FillColor red="255" green="255" blue="255"/>
                      <dmndi:StrokeColor red="0" green="0" blue="0"/>
                      <dmndi:FontColor red="0" green="0" blue="0"/>
                    </dmndi:DMNStyle>
                    <dc:Bounds x="594" y="227" width="166" height="50"/>
                    <dmndi:DMNLabel/>
                  </dmndi:DMNShape>
                  <dmndi:DMNShape id="dmnshape-drg-_82350973-C1B3-4748-B9D0-3D6EB7A6974E" dmnElementRef="_82350973-C1B3-4748-B9D0-3D6EB7A6974E" isCollapsed="false">
                    <dmndi:DMNStyle>
                      <dmndi:FillColor red="255" green="255" blue="255"/>
                      <dmndi:StrokeColor red="0" green="0" blue="0"/>
                      <dmndi:FontColor red="0" green="0" blue="0"/>
                    </dmndi:DMNStyle>
                    <dc:Bounds x="603" y="403" width="145" height="55"/>
                    <dmndi:DMNLabel/>
                  </dmndi:DMNShape>
                  <dmndi:DMNShape id="dmnshape-drg-_327E5C1D-CED1-4090-BB57-9459C2CC91E7" dmnElementRef="_327E5C1D-CED1-4090-BB57-9459C2CC91E7" isCollapsed="false">
                    <dmndi:DMNStyle>
                      <dmndi:FillColor red="255" green="255" blue="255"/>
                      <dmndi:StrokeColor red="0" green="0" blue="0"/>
                      <dmndi:FontColor red="0" green="0" blue="0"/>
                    </dmndi:DMNStyle>
                    <dc:Bounds x="848" y="227" width="100" height="50"/>
                    <dmndi:DMNLabel/>
                  </dmndi:DMNShape>
                  <dmndi:DMNEdge id="dmnedge-drg-_D783B9FF-7D94-4EF1-8E76-6EA33D63BFDB" dmnElementRef="_D783B9FF-7D94-4EF1-8E76-6EA33D63BFDB">
                    <di:waypoint x="897" y="433"/>
                    <di:waypoint x="677" y="277"/>
                  </dmndi:DMNEdge>
                  <dmndi:DMNEdge id="dmnedge-drg-_C32FBEB9-B062-4B4A-BCAB-5D1A2BCDF9E4" dmnElementRef="_C32FBEB9-B062-4B4A-BCAB-5D1A2BCDF9E4">
                    <di:waypoint x="675.5" y="430.5"/>
                    <di:waypoint x="677" y="277"/>
                  </dmndi:DMNEdge>
                  <dmndi:DMNEdge id="dmnedge-drg-_D0B6E7F2-D257-4173-B6EA-028DF1F291D6-AUTO-TARGET" dmnElementRef="_D0B6E7F2-D257-4173-B6EA-028DF1F291D6">
                    <di:waypoint x="897" y="433"/>
                    <di:waypoint x="920" y="227"/>
                  </dmndi:DMNEdge>
                </dmndi:DMNDiagram>
              </dmndi:DMNDI>
            </dmn:definitions>
            """
        }
}
