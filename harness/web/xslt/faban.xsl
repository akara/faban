<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xforms="http://www.w3.org/2002/xforms"
    xmlns:xlink="http://www.w3.org/1999/xlink"
    xmlns:chiba="http://chiba.sourceforge.net/xforms"
    exclude-result-prefixes="chiba xforms xlink xsl">

    <xsl:include href="html-form-controls.xsl"/>
    <xsl:output method="html" encoding="UTF-8"/>

    <xsl:param name="action-url" select="''"/>
    <xsl:param name="form-id" select="'chiba-form'"/>
    <xsl:param name="debug-enabled" select="'false'"/>

    <xsl:template match="/">
        <html>
            <xsl:apply-templates/>
        </html>
    </xsl:template>

    <xsl:template match="head">
        <head>
            <title>Faban Benchmark Configuration</title>
            <style type="text/css">
                <xsl:comment>
                    a {font-size:7pt;}
                    a:link {text-decoration:none; color:#000000;}
                    a:visited {text-decoration:none; color:#000000;}
                    a:hover {text-decoration:underline; color:#000000;}
                    a:active {text-decoration:underline; color:#000000;}
                    a:focus {text-decoration:underline; color:#000000;}
                    body {color:#000000; background-color:#FFFFFF; font-family:Verdana,Arial,Helvetica; font-size:10pt; margin-top:5%; margin-left:5%; margin-right:5%;}
                    input {color:#000000; background-color:#FFFFFF; font-family:Verdana,Arial,Helvetica; font-size:10pt; border-color:#000000; border-width:0pt; border-style:hidden;}
                    textarea {color:#000000; background-color:#FFFFFF; font-family:Verdana,Arial,Helvetica; font-size:10pt; border-color:#000000; border-width:0pt; border-style:hidden;}
                    select {color:#000000; background-color:#FFFFFF; font-family:Verdana,Arial,Helvetica; font-size:10pt; border-color:#000000; border-width:0pt; border-style:hidden;}
                    option {color:#000000; background-color:#FFFFFF; font-family:Verdana,Arial,Helvetica; font-size:10pt; border-color:#000000; border-width:0pt; border-style:hidden;}
                    table {border-width:1pt; border-color:#FFFFFF; border-style:solid; border-collapse:collapse; border-spacing:0pt; margin-top:5pt; width:100%;}
                    td {color:#000000; background-color:#A3B8CB; font-family:Verdana,Arial,Helvetica; font-size:10pt; padding-top:0pt; padding-bottom:5pt; padding-left:5pt; padding-right:5pt;}
                    .hidden-table {border-width:1pt; border-color:#A3B8CB; border-style:solid; border-collapse:collapse; border-spacing:0pt; width:100%;}
                    .active-tab {font-size:9pt; border-width:0pt; border-color:#FFFFFF; border-style:solid; border-bottom-style:hidden; padding-top:0pt; padding-bottom:0pt; padding-left:5pt; padding-right:5pt;}
		    .inactive-tab {border-width:1pt; border-color:#FFFFFF; border-style:solid; color:#FFFFFF; background-color:#5382A1; padding:0pt;}
		    .inactive-tab-button {font-size:9pt; color:#FFFFFF; background-color:#5382A1;}
                    .filler-tab {border-width:1pt; border-color:#FFFFFF; border-style:solid; border-top-style:hidden; border-right-style:hidden; width:99%; background-color:#FFFFFF; text-align:right; padding-left:5pt; padding-right:5pt;}
                    .group-label {border-width:1pt; border-color:#FFFFFF; border-style:solid; color:#FFFFFF; background-color:#5382A1; padding-left:5pt; padding-right:5pt; padding-top:2pt; padding-bottom:2pt; width:100%;}
                    .control-label {text-align:right; vertical-align:top; padding-left:5pt; padding-right:5pt padding-top:2pt; padding-bottom:2pt; width:30%;}
                    .control {text-align:left; vertical-align:top; padding-left:5pt; padding-right:5pt; padding-top:2pt; padding-bottom:2pt; width:70%;}
                    .action-panel {margin:5pt; text-align:center; vertical-align:middle;}
                    .action-button {color:#000000; background-color:#C5D5A9; font-family:Verdana,Arial,Helvetica; font-size:10pt; border-color:#7F7800; border-width:1pt; border-style:solid; margin:5pt;}
                    .alert {color:#FF0000; text-align:left; vertical-align:top; padding-top:2pt; padding-bottom:2pt; width:70%;}
                    .value-wide {width:80%;}
                </xsl:comment>
            </style>
        </head>
    </xsl:template>

    <xsl:template match="body">
        <body>
            <form name="{$form-id}" action="{$action-url}" method="post" enctype="application/x-www-form-urlencoded">
                <xsl:apply-templates/>
            </form>
        </body>
    </xsl:template>

    <xsl:template match="xforms:model"/>

    <xsl:template match="xforms:group[@id='group-tabsheet']">
        <xsl:variable name="selected-case" select="xforms:switch/xforms:case[@xforms:selected='true']"/>
        <table>
            <tr>
                <xsl:for-each select="xforms:trigger">
                    <xsl:choose>
                        <xsl:when test="xforms:action/xforms:toggle/@xforms:case=$selected-case/@id">
                            <td class="active-tab">
                                <xsl:apply-templates select="xforms:label"/>
                            </td>
                        </xsl:when>
                        <xsl:otherwise>
                            <td class="inactive-tab">
<!--                                <input type="submit" name="{chiba:data/@chiba:name}" value="{xforms:label}" class="inactive-tab-button"/>-->
                                <input type="submit" name="{concat('t_',@id)}" value="{xforms:label}" class="inactive-tab-button"/>
                            </td>
                        </xsl:otherwise>
                    </xsl:choose>
                </xsl:for-each>
                <td class="filler-tab">
                    <a href="http://pae.sfbay/external/jeap/projects/faban/">
                        <xsl:value-of disable-output-escaping="yes" select="'&amp;copy;'"/> 2005-2009 Faban
                    </a>
                </td>
            </tr>
            <xsl:variable name="tab-count" select="count(xforms:trigger) + 1"/>
            <xsl:for-each select="$selected-case/*">
                <tr>
                    <td colspan="{$tab-count}">
                        <xsl:apply-templates select="."/>
                    </td>
                </tr>
            </xsl:for-each>
        </table>
    </xsl:template>

    <xsl:template match="xforms:group[@id='group-buttons']">
        <div class="action-panel">
            <xsl:for-each select="xforms:trigger">
<!--                <input type="submit" name="{chiba:data/@chiba:name}" value="{xforms:label}" class="action-button"/>-->
                <input type="submit" name="{concat('t_',@id)}" value="{xforms:label}" class="action-button"/>
            </xsl:for-each>
        </div>
    </xsl:template>

    <xsl:template match="xforms:group[xforms:label]">
        <table>
            <tr>
                <td class="group-label" colspan="2">
                    <xsl:apply-templates select="xforms:label"/>
                </td>
            </tr>
            <xsl:for-each select="xforms:input|xforms:select1|xforms:textarea">
                <tr>
                    <td class="control-label">
                        <xsl:apply-templates select="xforms:label"/>
                    </td>
                    <td class="control">
                        <xsl:apply-templates select="."/>
                    </td>
                </tr>
            </xsl:for-each>
        </table>
    </xsl:template>

    <xsl:template match="xforms:group">
        <table class="hidden-table">
            <xsl:for-each select="xforms:input|xforms:select1|xforms:textarea">
                <tr>
                    <td class="control-label">
                        <xsl:apply-templates select="xforms:label"/>
                    </td>
                    <td class="control">
                        <xsl:apply-templates select="."/>
                    </td>
                </tr>
            </xsl:for-each>
        </table>
    </xsl:template>

    <xsl:template match="xforms:select1">
        <xsl:call-template name="select1">
<!--            <xsl:with-param name="name" select="chiba:data/@chiba:name"/>-->
            <xsl:with-param name="name" select="concat('d_',@id)"/>
            <xsl:with-param name="value" select="chiba:data"/>
            <xsl:with-param name="appearance" select="@xforms:appearance"/>
        </xsl:call-template>
    </xsl:template>

    <xsl:template match="xforms:input">
        <xsl:call-template name="input">
<!--            <xsl:with-param name="name" select="chiba:data/@chiba:name"/>-->
            <xsl:with-param name="name" select="concat('d_',@id)"/>
            <xsl:with-param name="value" select="chiba:data"/>
            <xsl:with-param name="size" select="40"/>
        </xsl:call-template>
        <xsl:apply-templates select="xforms:alert"/>
    </xsl:template>

    <xsl:template match="xforms:textarea">
        <xsl:call-template name="textarea">
            <xsl:with-param name="name" select="concat('d_',@id)"/>
            <xsl:with-param name="value" select="chiba:data"/>
            <xsl:with-param name="rows" select="10"/>
            <xsl:with-param name="cols" select="40"/>
        </xsl:call-template>
    </xsl:template>

    <xsl:template match="xforms:label">
        <xsl:copy-of select="*|text()"/>
    </xsl:template>

    <xsl:template match="xforms:alert[../chiba:data/@chiba:valid='false']">
        <div class="alert">
            <xsl:copy-of select="*|text()"/>
        </div>
    </xsl:template>

    <xsl:template match="xforms:alert"/>

</xsl:stylesheet>
