<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                              xmlns:fa="http://faban.sunsource.net/ns/faban"
                              xmlns:fh="http://faban.sunsource.net/ns/fabanharness">
    <xsl:output method="text"/>
    <xsl:param name="outputDir">oDir</xsl:param>
    <xsl:template match="/specweb2005Benchmark">

        <xsl:text>CLIENTS = "</xsl:text> <xsl:value-of select="fa:runConfig/fa:hostConfig/fa:hostPorts"/><xsl:text> "</xsl:text>

        <xsl:text>&#10;&#10;SIMULTANEOUS_SESSIONS = </xsl:text> <xsl:value-of select="fa:runConfig/fa:scale"/>

        <xsl:text>&#10;&#10;TEST_TYPE = </xsl:text> <xsl:value-of select="fa:runConfig/testtype"/>

        <xsl:text>&#10;&#10;WEB_SERVER = </xsl:text> <xsl:value-of select="webserverConfig/fa:hostConfig/fa:host"/>
        <xsl:text>&#10;WEB_PORT = </xsl:text> <xsl:value-of select="webserverConfig/wsConfig/wsPort"/>
        <xsl:text>&#10;SSL_PORT = </xsl:text> <xsl:value-of select="webserverConfig/wsConfig/wsSSLport"/>

        <xsl:text>&#10;&#10;BESIM_SERVER = </xsl:text> <xsl:value-of select="besimserverConfig/fa:hostConfig/fa:host"/>
        <xsl:text>&#10;BESIM_PORT = </xsl:text> <xsl:value-of select="besimserverConfig/besimConfig/port"/>
        <xsl:text>&#10;BESIM_INIT_SCRIPT = </xsl:text> <xsl:value-of select="besimserverConfig/besimConfig/initScript"/>
        <xsl:text>&#10;BESIM_PERSISTENT = </xsl:text> <xsl:value-of select="besimserverConfig/besimConfig/persistent"/>

        <xsl:text>&#10;&#10;BEAT_INTERVAL = 10000</xsl:text>

        <xsl:text>&#10;&#10;SMARTY_DIR = "/www/Smarty-2.6.7/libs/"</xsl:text>
        <xsl:text>&#10;SMARTY_BANK_DIR = "/www/bank/"</xsl:text>
        <xsl:text>&#10;SMARTY_ECOMMERCE_DIR = "/www/ecommerce/"</xsl:text>
        <xsl:text>&#10;SMARTY_SUPPORT_DIR = "/www/support/"</xsl:text>
        <xsl:text>&#10;SEND_CONTENT_LENGTH = 1</xsl:text>

        <xsl:text>&#10;&#10;HTTP_PROTOCOL = "HTTP/1.1"</xsl:text>

        <xsl:text>&#10;&#10;WARMUP_SECONDS = </xsl:text> <xsl:value-of select="fa:runConfig/warmupSeconds"/>
        <xsl:text>&#10;THREAD_RAMPUP_SECONDS = </xsl:text> <xsl:value-of select="fa:runConfig/threadRampupSeconds"/>
        <xsl:text>&#10;THREAD_RAMPDOWN_SECONDS = </xsl:text> <xsl:value-of select="fa:runConfig/fa:runControl/fa:rampDown"/>
        <xsl:text>&#10;RUN_SECONDS = </xsl:text> <xsl:value-of select="fa:runConfig/fa:runControl/fa:steadyState"/>

        <xsl:text>&#10;&#10;RAMPUP_SECONDS = </xsl:text> <xsl:value-of select="fa:runConfig/rampupSeconds"/>
        <xsl:text>&#10;RAMPDOWN_SECONDS = </xsl:text> <xsl:value-of select="fa:runConfig/rampdownSeconds"/>

        <xsl:text>&#10;WAIT_TO_BEGIN = 1</xsl:text>
        <xsl:text>&#10;KILL_CLIENT = 0</xsl:text>
        <xsl:text>&#10;DEBUG_LEVEL = 0</xsl:text>
        <xsl:text>&#10;LANGUAGE = EN</xsl:text>
        <xsl:text>&#10;COUNTRY = US</xsl:text>

        <xsl:text>&#10;ITERATIONS = </xsl:text> <xsl:value-of select="fa:runConfig/iterations"/>
        <xsl:text>&#10;PARALLEL_CONNS = 2</xsl:text>
        <xsl:text>&#10;SLEEP_TEST_ITERATIONS = 20</xsl:text>
        <xsl:text>&#10;OVERLOAD_RUN = 0</xsl:text>
        <xsl:text>&#10;MAX_OVERTHINK_TIME = 20000</xsl:text>
        <xsl:text>&#10;OVERLOAD_FACTOR = 1.6</xsl:text>
        <xsl:text>&#10;SSL_PROTOCOL = "SSLv3"</xsl:text>
        <xsl:text>&#10;SSL_CIPHER = "SSL_RSA_WITH_RC4_128_MD5"</xsl:text>
        <xsl:text>&#10;SSL_REQUEST_PERCENT = 0</xsl:text>
        <xsl:text>&#10;COND_GET_REQ_PERCENT = 0</xsl:text>

        <xsl:text>&#10;MIN_REQUESTS_PER_KEEP_ALIVE = 5</xsl:text>
        <xsl:text>&#10;MAX_REQUESTS_PER_KEEP_ALIVE = 15</xsl:text>
        <xsl:text>&#10;PERCENT_KEEP_ALIVE_REQUESTS = 70</xsl:text>
        <xsl:text>&#10;PROTOCOL_TIMEOUT_SECONDS = 60</xsl:text>
        <xsl:text>&#10;DATA_RATE_DISTRIBUTION = "10MB_LAN,0.0%; CABLE,100.0%; FAST,0.0%; SW99,0.0%"</xsl:text>
        <xsl:text>&#10;SW99_DATA_RATE = 50000</xsl:text>
        <xsl:text>&#10;FAST_MODEM_DATA_RATE = 6000</xsl:text>
        <xsl:text>&#10;HIGH_SPEED_DATA_RATE = 100000</xsl:text>
        <xsl:text>&#10;LAN_DATA_RATE = 1000000</xsl:text>


    </xsl:template>

</xsl:stylesheet>
