<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
    <xsl:template match="/benchResults">
        <html>
            <head>
                <title><xsl:value-of select="benchSummary/@name"/>
                    <xsl:text> </xsl:text>
                    <xsl:value-of select="benchSummary/@version"/>
                    Summary Report</title>
		<meta http-equiv="content-type" content="text/html;charset=iso-8859-1"/>
            </head>
            <body>
                <h2 style="text-align: center;"><xsl:value-of
                    select="benchSummary/@name"/>
                    <xsl:text> </xsl:text>
                    <xsl:value-of select="benchSummary/@version"/>
                    <br></br>Summary Report</h2><br></br>
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
                    <hr></hr>
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
                    <h3>Operation Mix</h3>
                    <table border="1" cellpadding="2" cellspacing="0"
                        style="text-align: center; width: 100%;">
                        <tbody>
                            <tr style="vertical-align: top;">
                                <th style="text-align: left;">Type</th>
                                <th>Success<br></br>Count</th>
                                <th>Failure<br></br>Count</th>
                                <th>Mix</th>
                                <th>Required Mix<br></br>
                                    (<xsl:value-of select='format-number(mix/@allowedDeviation, "##.##%")'/> deviation allowed)</th>
                                <th>Pass/Fail</th>
                            </tr>
                            <xsl:for-each select="mix/operation">
                                <tr>
                                    <td style="text-align: left;"><xsl:value-of select="@name"/></td>
                                    <td><xsl:value-of select="successes"/></td>
                                    <td><xsl:value-of select="failures"/></td>
                                    <td><xsl:value-of select='format-number(mix, "##.##%")'/></td>
                                    <td><xsl:value-of select='format-number(requiredMix, "##.##%")'/></td>
                                    <xsl:choose>
                                        <xsl:when test="passed='true'">
                                            <td style="color: rgb(0, 192, 0);">PASSED</td>
                                        </xsl:when>
                                        <xsl:otherwise>
                                            <td style="color: rgb(255, 0, 0);">FAILED</td>
                                        </xsl:otherwise>
                                    </xsl:choose>
                                </tr>
                            </xsl:for-each>
                        </tbody>
                    </table><br></br>
                    <h3>Response Times</h3>
                    <table border="1" cellpadding="2" cellspacing="0"
                        style="text-align: center; width: 100%;">
                        <tbody>
                            <tr style="vertical-align: top;">
                                <th style="text-align: left;">Type</th>
                                <th>Avg</th>
                                <th>Max</th>
                                <th>90th%</th>
                                <th>Reqd. 90th%</th>
                                <th>Pass/Fail</th>
                            </tr>
                            <xsl:for-each select="responseTimes/operation">
                                <tr>
                                    <td style="text-align: left;"><xsl:value-of select="@name"/></td>
                                    <td><xsl:value-of select="avg"/></td>
                                    <td><xsl:value-of select="max"/></td>
                                    <td><xsl:value-of select="p90th"/></td>
                                    <td><xsl:value-of select="@r90th"/></td>
                                    <xsl:choose>
                                        <xsl:when test="passed='true'">
                                            <td style="color: rgb(0, 192, 0);">PASSED</td>
                                        </xsl:when>
                                        <xsl:otherwise>
                                            <td style="color: rgb(255, 0, 0);">FAILED</td>
                                        </xsl:otherwise>
                                    </xsl:choose>
                                </tr>
                            </xsl:for-each>
                        </tbody>
                    </table><br></br>
                    <h3>Cycle/Think Times</h3>
                    <table border="1" cellpadding="2" cellspacing="0"
                        style="text-align: center; width: 100%;">
                        <tbody>
                            <tr style="vertical-align: top;">
                                <th style="text-align: left;">Type</th>
                                <th>Targeted Avg</th>
                                <th>Actual Avg</th>
                                <th>Min</th>
                                <th>Max</th>
                                <th>Pass/Fail</th>
                            </tr>
                            <xsl:for-each select="delayTimes/operation">
                                <tr>
                                    <td style="text-align: left;"><xsl:value-of select="@name"/></td>
                                    <td><xsl:value-of select="targetedAvg"/></td>
                                    <td><xsl:value-of select="actualAvg"/></td>
                                    <td><xsl:value-of select="min"/></td>
                                    <td><xsl:value-of select="max"/></td>
                                    <xsl:choose>
                                        <xsl:when test="passed='true'">
                                            <td style="color: rgb(0, 192, 0);">PASSED</td>
                                        </xsl:when>
                                        <xsl:otherwise>
                                            <td style="color: rgb(255, 0, 0);">FAILED</td>
                                        </xsl:otherwise>
                                    </xsl:choose>
                                </tr>
                            </xsl:for-each>
                        </tbody>
                    </table><br></br>
                    <xsl:if test="miscStats">
                    <h3>Miscellaneous Statistics</h3>
                    <table border="1" cellpadding="2" cellspacing="0"
                        style="text-align: center; width: 100%;">
                        <tbody>
                            <tr style="vertical-align: top;">
                                <th style="text-align: left;">Description</th>
                                <th>Results</th>
                                <th>Targeted<br></br>Results</th>
                                <th>Allowed<br></br>Deviation</th>
                                <th>Pass/Fail</th>
                            </tr>
                            <xsl:for-each select="miscStats/stat">
                                <tr>
                                    <td style="text-align: left;"><xsl:value-of select="description"/></td>
                                    <td><xsl:value-of select="result"/></td>
                                    <td><xsl:value-of select="target"/></td>
                                    <td><xsl:value-of select="allowedDeviation"/></td>
                                    <xsl:choose>
                                        <xsl:when test="passed='true'">
                                            <td style="color: rgb(0, 192, 0);">PASSED</td>
                                        </xsl:when>
                                        <xsl:when test="passed='false'">
                                            <td style="color: rgb(255, 0, 0);">FAILED</td>
                                        </xsl:when>
                                        <xsl:otherwise>
                                            <td></td>
                                        </xsl:otherwise>
                                    </xsl:choose>
                                </tr>
                            </xsl:for-each>
                        </tbody>
                    </table><br></br>
                    </xsl:if>
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
                </xsl:for-each>
                <br></br><hr></hr>
            </body>
        </html>
    </xsl:template>
</xsl:stylesheet>

