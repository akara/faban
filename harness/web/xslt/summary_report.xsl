<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
    <xsl:template match="/benchResults">
        <html>
            <head>
                <title><xsl:value-of select="benchSummary/@name"/>
                    <xsl:text> </xsl:text>
                    <xsl:value-of select="benchSummary/@version"/>
                    <xsl:choose>
                        <xsl:when test="benchSummary/@host">
                            Partial Summary for Driver Host
                            <xsl:text> </xsl:text>
                            <xsl:value-of select="benchSummary/@host"/>
                        </xsl:when>
                        <xsl:otherwise>
                            Summary Report
                        </xsl:otherwise>
                    </xsl:choose>
                </title>
                <link rel="icon" type="image/gif" href="/img/faban.gif"/>
                <link rel="stylesheet" type="text/css" href="/css/style.css" />
            </head>
            <body>
                <h2 style="text-align: center;"><xsl:value-of
                    select="benchSummary/@name"/><br></br>
                    <xsl:text> </xsl:text>
                    <xsl:value-of select="benchSummary/@version"/>
                    <xsl:choose>
                         <xsl:when test="benchSummary/@host">
                            <br></br>Partial Summary for Driver Host
                            <xsl:text> </xsl:text>
                            <xsl:value-of select="benchSummary/@host"/>
                        </xsl:when>
                        <xsl:otherwise>
                            <br></br>Summary Report
                        </xsl:otherwise>
                    </xsl:choose>
                    </h2><br></br>
                <table border="0" cellpadding="2" cellspacing="2">
                    <tbody>
                        <tr>
                            <td><xsl:value-of
                                select="benchSummary/@name"/>
                                    metric:
                            </td>
                            <td><xsl:value-of
                                select="benchSummary/metric"/>
                                <xsl:text> </xsl:text>
                                <xsl:value-of
                                select="benchSummary/metric/@unit"/>
                            </td>
                        </tr>
                        <tr>
                            <td>Benchmark Start:</td>
                            <td><xsl:value-of
                                select="benchSummary/startTime"/></td>
                        </tr>
                        <tr>
                            <td>Benchmark End:</td>
                            <td><xsl:value-of
                                select="benchSummary/endTime"/></td>
                        </tr>
                        <tr>
                            <td>Run ID:</td>
                            <td><xsl:value-of
                                select="benchSummary/runId"/></td>
                        </tr>
                        <tr>
                            <td>Pass/Fail:</td>
                            <xsl:choose>
                                <xsl:when test="benchSummary/passed='true'">
                                    <td style="color: rgb(0, 192, 0);">PASSED</td>
                                </xsl:when>
                                <xsl:otherwise>
                                    <td style="color: rgb(255, 0, 0);">FAILED</td>
                                </xsl:otherwise>
                            </xsl:choose>
                        </tr>
                        <tr>
                            <td style="vertical-align: top;">Active Drivers:</td>
                            <td style="vertical-align: top;">
                                <xsl:for-each select="driverSummary">
                                    <xsl:variable name="driverName" select="@name"/>
                                    <a href="#{$driverName}"><xsl:value-of select="@name"/></a>
                                    <br></br>
                                </xsl:for-each>
                            </td>
                        </tr>
                    </tbody>
                </table>
                <xsl:for-each select="driverSummary">
                    <br></br>
                    <hr style="border: 1px solid #cccccc;"></hr>
                    <h2 style="text-align: center;">
                        <xsl:variable name="driverName" select="@name"/>
                        <a name="{$driverName}"><xsl:value-of select="@name"/></a>
                    </h2>
                    <br></br>
                    <table>
                        <tbody>
                            <tr>
                                <td>
                                    <xsl:value-of select="@name"/>
                                    <xsl:text> metric:</xsl:text>
                                </td>
                                <td>
                                    <xsl:value-of select="metric"/>
                                    <xsl:text> </xsl:text>
                                    <xsl:value-of select="metric/@unit"/>
                                </td>
                            </tr>
                            <tr>
                                <td>Driver start:</td>
                                <td><xsl:value-of select="startTime"/></td>
                            </tr>
                            <tr>
                                <td>Driver end:</td>
                                <td><xsl:value-of select="endTime"/></td>
                            </tr>
                            <tr>
                                <td>Total number of <xsl:value-of select="totalOps/@unit"/>:
                                </td>
                                <td><xsl:value-of select="totalOps"/></td>
                            </tr>
                            <tr>
                                <td>Pass/Fail:</td>
                                <xsl:choose>
                                    <xsl:when test="passed='true'">
                                        <td style="color: rgb(0, 192, 0);">PASSED</td>
                                    </xsl:when>
                                    <xsl:otherwise>
                                        <td style="color: rgb(255, 0, 0);">FAILED</td>
                                    </xsl:otherwise>
                                </xsl:choose>
                            </tr>
                        </tbody>
                    </table><br></br>
                    <xsl:if test="mix">
                    <h3>Operation Mix</h3>
                    <table border="0" cellpadding="4" cellspacing="3"
                        style="padding: 2px; border: 2px solid #cccccc; text-align: center; width: 100%;">
                        <tbody>
                            <tr style="vertical-align: top;">
                                <th class="header" style="text-align: left;">Type</th>
                                <th class="header">Success<br></br>Count</th>
                                <th class="header">Failure<br></br>Count</th>
                                <th class="header">Mix</th>
                                <th class="header">Required Mix<br></br>
                                    (<xsl:value-of select='format-number(mix/@allowedDeviation, "##.##%")'/> deviation allowed)</th>
                                <th class="header">Pass/Fail</th>
                            </tr>
                            <xsl:for-each select="mix/operation">
                                <tr>
                                    <xsl:choose>
                                        <xsl:when test="(position() mod 2 = 1)">
                                            <xsl:attribute name="class">even</xsl:attribute>
                                        </xsl:when>
                                        <xsl:otherwise>
                                            <xsl:attribute name="class">odd</xsl:attribute>
                                        </xsl:otherwise>
                                    </xsl:choose>
                                    <td class="tablecell" style="text-align: left;"><xsl:value-of select="@name"/></td>
                                    <td class="tablecell"><xsl:value-of select="successes"/></td>
                                    <td class="tablecell"><xsl:value-of select="failures"/></td>
                                    <td class="tablecell"><xsl:value-of select='format-number(mix, "##.##%")'/></td>
                                    <td class="tablecell"><xsl:value-of select='format-number(requiredMix, "##.##%")'/></td>
                                    <xsl:choose>
                                        <xsl:when test="passed='true'">
                                            <td class="tablecell" style="color: rgb(0, 192, 0);">PASSED</td>
                                        </xsl:when>
                                        <xsl:when test="passed='false'">
                                            <td class="tablecell" style="color: rgb(255, 0, 0);">FAILED</td>
                                        </xsl:when>
                                        <xsl:otherwise>
                                            <td class="tablecell"></td>
                                        </xsl:otherwise>
                                    </xsl:choose>
                                </tr>
                            </xsl:for-each>
                        </tbody>
                    </table><br></br>
                    </xsl:if>
                    <xsl:if test="responseTimes">
                    <h3>Response Times
                        <xsl:if test="responseTimes/@unit">
                            (<xsl:value-of select="responseTimes/@unit"/>)
                        </xsl:if>
                    </h3>
                    <table border="0" cellpadding="4" cellspacing="3"
                        style="padding: 2px; border: 2px solid #cccccc; text-align: center; width: 100%;">
                        <tbody>
                            <tr style="vertical-align: top;">
                                <th class="header" style="text-align: left;">Type</th>
                                <th class="header">Avg</th>
                                <th class="header">Max</th>
                                <th class="header">SD</th>
                                <xsl:if test="responseTimes/operation[1]/percentile">
                                    <xsl:for-each select="responseTimes/operation[1]/percentile">
                                        <th class="header"><xsl:value-of select="@nth"/><xsl:value-of select="@suffix"/>%</th>
                                        <xsl:variable name="pct" select="@nth"/>
                                        <xsl:if test="../../operation/percentile[@nth=$pct]/@limit">
                                            <th class="header"><xsl:value-of select="@nth"/><xsl:value-of select="@suffix"/>%<br/>limit</th>
                                        </xsl:if>
                                    </xsl:for-each>
                                </xsl:if>
                                <xsl:if test="responseTimes/operation[1]/@r90th">
                                    <th class="header">90th%</th>
                                    <th class="header">Reqd. 90th%</th>
                                </xsl:if>
                                <th class="header">Pass/Fail</th>
                            </tr>
                            <xsl:for-each select="responseTimes/operation">
                                <tr>
                                    <xsl:choose>
                                        <xsl:when test="(position() mod 2 = 1)">
                                            <xsl:attribute name="class">even</xsl:attribute>
                                        </xsl:when>
                                        <xsl:otherwise>
                                            <xsl:attribute name="class">odd</xsl:attribute>
                                        </xsl:otherwise>
                                    </xsl:choose>
                                    <td class="tablecell" style="text-align: left;"><xsl:value-of select="@name"/></td>
                                    <td class="tablecell"><xsl:value-of select="avg"/></td>
                                    <td class="tablecell"><xsl:value-of select="max"/></td>
                                    <td class="tablecell"><xsl:value-of select="sd"/></td>
                                    <xsl:if test="@r90th">
                                        <td class="tablecell"><xsl:value-of select="p90th"/></td>
                                        <td class="tablecell"><xsl:value-of select="@r90th"/></td>
                                    </xsl:if>
                                    <xsl:if test="percentile">
                                        <xsl:for-each select="percentile">
                                            <td class="tablecell"><xsl:value-of select="."/></td>
                                        <xsl:variable name="pct" select="@nth"/>
                                        <xsl:if test="../../operation/percentile[@nth=$pct]/@limit">
                                            <td class="tablecell"><xsl:value-of select="@limit"/></td>
                                        </xsl:if>
                                        </xsl:for-each>
                                    </xsl:if>
                                    <xsl:choose>
                                        <xsl:when test="passed='true'">
                                            <td class="tablecell" style="color: rgb(0, 192, 0);">PASSED</td>
                                        </xsl:when>
                                        <xsl:when test="passed='false'">
                                            <td class="tablecell" style="color: rgb(255, 0, 0);">FAILED</td>
                                        </xsl:when>
                                        <xsl:otherwise>
                                            <td class="tablecell"></td>
                                        </xsl:otherwise>
                                    </xsl:choose>
                                </tr>
                            </xsl:for-each>
                        </tbody>
                    </table><br></br>
                    </xsl:if>
                    <xsl:if test="delayTimes">
                    <h3>Cycle/Think Times (seconds)</h3>
                    <table border="0" cellpadding="4" cellspacing="3"
                        style="padding: 2px; border: 2px solid #cccccc; text-align: center; width: 100%;">
                        <tbody>
                            <tr style="vertical-align: top;">
                                <th class="header" style="text-align: left;">Type</th>
                                <th class="header">Targeted Avg</th>
                                <th class="header">Actual Avg</th>
                                <th class="header">Min</th>
                                <th class="header">Max</th>
                                <th class="header">Pass/Fail</th>
                            </tr>
                            <xsl:for-each select="delayTimes/operation">
                                <tr>
                                    <xsl:choose>
                                        <xsl:when test="(position() mod 2 = 1)">
                                            <xsl:attribute name="class">even</xsl:attribute>
                                        </xsl:when>
                                        <xsl:otherwise>
                                            <xsl:attribute name="class">odd</xsl:attribute>
                                        </xsl:otherwise>
                                    </xsl:choose>
                                    <td class="tablecell" style="text-align: left;"><xsl:value-of select="@name"/></td>
                                    <td class="tablecell"><xsl:value-of select="targetedAvg"/></td>
                                    <td class="tablecell"><xsl:value-of select="actualAvg"/></td>
                                    <td class="tablecell"><xsl:value-of select="min"/></td>
                                    <td class="tablecell"><xsl:value-of select="max"/></td>
                                    <xsl:choose>
                                        <xsl:when test="passed='true'">
                                            <td class="tablecell" style="color: rgb(0, 192, 0);">PASSED</td>
                                        </xsl:when>
                                        <xsl:when test="passed='false'">
                                            <td class="tablecell" style="color: rgb(255, 0, 0);">FAILED</td>
                                        </xsl:when>
                                        <xsl:otherwise>
                                            <td class="tablecell"></td>
                                        </xsl:otherwise>
                                    </xsl:choose>
                                </tr>
                            </xsl:for-each>
                        </tbody>
                    </table><br></br>
                    </xsl:if>
                    <xsl:if test="miscStats">
                    <h3>Miscellaneous Statistics</h3>
                    <table border="0" cellpadding="4" cellspacing="3"
                        style="padding: 2px; border: 2px solid #cccccc; text-align: center; width: 100%;">
                        <tbody>
                            <tr style="vertical-align: top;">
                                <th class="header" style="text-align: left;">Description</th>
                                <th class="header">Results</th>
                                <xsl:if test="miscStats/stat/target">
                                    <th class="header">Targeted<br/>Results</th>
                                </xsl:if>
                                <xsl:if test="miscStats/stat/allowedDeviation">
                                    <th class="header">Allowed<br/>Deviation</th>
                                </xsl:if>
                                <th class="header">Pass/Fail</th>
                            </tr>
                            <xsl:for-each select="miscStats/stat">
                                <tr>
                                    <xsl:choose>
                                        <xsl:when test="(position() mod 2 = 1)">
                                            <xsl:attribute name="class">even</xsl:attribute>
                                        </xsl:when>
                                        <xsl:otherwise>
                                            <xsl:attribute name="class">odd</xsl:attribute>
                                        </xsl:otherwise>
                                    </xsl:choose>
                                    <td class="tablecell" style="text-align: left;"><xsl:value-of select="description"/></td>
                                    <td class="tablecell"><xsl:value-of select="result"/></td>
                                    <xsl:if test="../stat/target">
                                        <td class="tablecell"><xsl:value-of select="target"/></td>
                                    </xsl:if>
                                    <xsl:if test="../stat/allowedDeviation">
                                        <td class="tablecell"><xsl:value-of select="allowedDeviation"/></td>
                                    </xsl:if>
                                    <xsl:if test="../stat/passed">
                                        <xsl:choose>
                                            <xsl:when test="passed='true'">
                                                <td class="tablecell" style="color: rgb(0, 192, 0);">PASSED</td>
                                            </xsl:when>
                                            <xsl:when test="passed='false'">
                                                <td class="tablecell" style="color: rgb(255, 0, 0);">FAILED</td>
                                            </xsl:when>
                                            <xsl:otherwise>
                                                <td class="tablecell"></td>
                                            </xsl:otherwise>
                                        </xsl:choose>
                                    </xsl:if>
                                </tr>
                            </xsl:for-each>
                        </tbody>
                    </table><br></br>
                    </xsl:if>
                    <xsl:for-each select="customStats">
                    <h3><xsl:value-of select="@name"/></h3>
                    <table border="0" cellpadding="4" cellspacing="3"
                        style="padding: 2px; border: 2px solid #cccccc; text-align: center; width: 100%;">
                        <tbody>
                            <tr style="vertical-align: top;">
                                <th class="header" style="text-align: left;">Description</th>
                                <th class="header">Results</th>
                                <xsl:if test="stat/target">
                                    <th class="header">Targeted<br></br>Results</th>
                                </xsl:if>
                                <xsl:if test="stat/allowedDeviation">
                                    <th class="header">Allowed<br></br>Deviation</th>
                                </xsl:if>
                                <xsl:if test="stat/passed">
                                    <th class="header">Pass/Fail</th>
                                </xsl:if>
                            </tr>
                            <xsl:for-each select="stat">
                                <tr>
                                    <xsl:choose>
                                        <xsl:when test="(position() mod 2 = 1)">
                                            <xsl:attribute name="class">even</xsl:attribute>
                                        </xsl:when>
                                        <xsl:otherwise>
                                            <xsl:attribute name="class">odd</xsl:attribute>
                                        </xsl:otherwise>
                                    </xsl:choose>
                                    <td class="tablecell" style="text-align: left;"><xsl:value-of select="description"/></td>
                                    <td class="tablecell"><xsl:value-of select="result"/></td>
                                    <xsl:if test="../stat/target">
                                        <td class="tablecell"><xsl:value-of select="target"/></td>
                                    </xsl:if>
                                    <xsl:if test="../stat/allowedDeviation">
                                        <td class="tablecell"><xsl:value-of select="allowedDeviation"/></td>
                                    </xsl:if>
                                    <xsl:if test="../stat/passed">
                                        <xsl:choose>
                                            <xsl:when test="passed='true'">
                                                <td class="tablecell" style="color: rgb(0, 192, 0);">PASSED</td>
                                            </xsl:when>
                                            <xsl:when test="passed='false'">
                                                <td class="tablecell" style="color: rgb(255, 0, 0);">FAILED</td>
                                            </xsl:when>
                                            <xsl:otherwise>
                                                <td class="tablecell"></td>
                                            </xsl:otherwise>
                                        </xsl:choose>
                                    </xsl:if>
                                </tr>
                            </xsl:for-each>
                        </tbody>
                    </table><br></br>
                    </xsl:for-each>
                    <xsl:for-each select="customTable">
                    <h3><xsl:value-of select="@name"/></h3>
                    <table border="0" cellpadding="4" cellspacing="3"
                        style="padding: 2px; border: 2px solid #cccccc; text-align: center; width: 100%;">
                        <tbody>
                            <tr style="vertical-align: top;">
                                <xsl:for-each select="head/th">
                                    <th class="header"><xsl:value-of select="."/></th>
                                </xsl:for-each>
                            </tr>
                            <xsl:for-each select="tr">
                                <tr>
                                    <xsl:choose>
                                        <xsl:when test="(position() mod 2 = 1)">
                                            <xsl:attribute name="class">even</xsl:attribute>
                                        </xsl:when>
                                        <xsl:otherwise>
                                            <xsl:attribute name="class">odd</xsl:attribute>
                                        </xsl:otherwise>
                                    </xsl:choose>
                                    <xsl:apply-templates select="node()"/>
                                </tr>
                            </xsl:for-each>
                        </tbody>
                    </table><br></br>
                    </xsl:for-each>
                    <xsl:if test="users">
                        <xsl:if test="rtXtps">
                            <h3>Little's Law Verification</h3>
                            <table border="0" cellpadding="2" cellspacing="2">
                                <tbody>
                                    <tr>
                                        <td>Number of users</td>
                                        <td>=</td>
                                        <td><xsl:value-of select="users"/></td>
                                    </tr>
                                    <tr>
                                        <td>Sum of Avg. RT * TPS for all Tx Types</td>
                                        <td>=</td>
                                        <td><xsl:value-of select="rtXtps"/></td>
                                    </tr>
                                </tbody>
                            </table>
                        </xsl:if>
                    </xsl:if>
                </xsl:for-each>
                <br></br>
                <hr style="border: 1px solid #cccccc;"></hr>
            </body>
        </html>
    </xsl:template>
    <xsl:template match="td|th">
        <xsl:copy>
          <xsl:attribute name="class">tablecell</xsl:attribute>
          <xsl:copy-of select="node()"/>
        </xsl:copy>
    </xsl:template>
</xsl:stylesheet>
