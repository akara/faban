<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
    <xsl:output method="text"/>
    <xsl:param name="outputDir">oDir</xsl:param>
    <xsl:template match="/specweb2005Benchmark">
        <xsl:text>DYNAMIC_SCRIPT = </xsl:text> <xsl:value-of select="supportParameters/supportWorkloadDetail/dynScript"/>
        <xsl:text>&#10;&#10;INIT_SCRIPT = </xsl:text> <xsl:value-of select="supportParameters/supportWorkloadDetail/initScript"/>
        <xsl:text>&#10;&#10;IMG_PATH = </xsl:text> <xsl:value-of select="supportParameters/supportWorkloadDetail/imgPath"/>
        <xsl:text>&#10;&#10;DYN_SCRIPT_PATH = </xsl:text> <xsl:value-of select="supportParameters/supportWorkloadDetail/dynScriptPath"/>
        <xsl:text>&#10;&#10;DOWNLOAD_PATH = </xsl:text> <xsl:value-of select="supportParameters/supportWorkloadDetail/downloadPath"/>
        <xsl:text>&#10;&#10;PADDING_DIR = </xsl:text> <xsl:value-of select="supportParameters/supportWorkloadDetail/paddingDir"/>
        <xsl:text>&#10;&#10;DOWNLOAD_SUBDIRS = </xsl:text> <xsl:value-of select="supportParameters/supportWorkloadDetail/downloadSubdirs"/>
        <xsl:text>&#10;BASE_SUBDIR_NAME = </xsl:text> <xsl:value-of select="supportParameters/supportWorkloadDetail/baseSubdir"/>
        <xsl:text>&#10;FIRST_SUBDIR_NUM = </xsl:text> <xsl:value-of select="supportParameters/supportWorkloadDetail/firstSubdirNum"/>
        <xsl:text>&#10;&#10;DYN_SCRIPT_0 = </xsl:text> <xsl:value-of select="supportParameters/supportWorkloadDetail/dynScript0"/>
        <xsl:text>&#10;DYN_SCRIPT_1 = </xsl:text> <xsl:value-of select="supportParameters/supportWorkloadDetail/dynScript1"/>
        <xsl:text>&#10;DYN_SCRIPT_2 = </xsl:text> <xsl:value-of select="supportParameters/supportWorkloadDetail/dynScript2"/>
        <xsl:text>&#10;DYN_SCRIPT_3 = </xsl:text> <xsl:value-of select="supportParameters/supportWorkloadDetail/dynScript3"/>
        <xsl:text>&#10;DYN_SCRIPT_4 = </xsl:text> <xsl:value-of select="supportParameters/supportWorkloadDetail/dynScript4"/>
        <xsl:text>&#10;DYN_SCRIPT_5 = </xsl:text> <xsl:value-of select="supportParameters/supportWorkloadDetail/dynScript5"/>
        <xsl:text>&#10;DYN_SCRIPT_6 = </xsl:text> <xsl:value-of select="supportParameters/supportWorkloadDetail/dynScript6"/>
        <xsl:text>&#10;&#10;LG_BUF_SIZE = </xsl:text> <xsl:value-of select="supportParameters/supportWorkloadDetail/bufSize"/>


        <xsl:text>
########################################################################
# Dynamic request commands for each page
DYN_CMD_0 = "home"
DYN_CMD_1 = "search"
DYN_CMD_2 = "catalog"
DYN_CMD_3 = "product"
DYN_CMD_4 = "fileCatalog"
DYN_CMD_5 = "file"
DYN_CMD_6 = "download"

# Mean wait time (in seconds) between requests
THINK_TIME = 5

# Measured value for percentage of requests using think time
THINK_TIME_REQ_PCT = 92.08

# Max wait time (in seconds) between requests; should be evenly
# divisible by THINK_INTERVAL
# (note: effectively, max think time will not be more than ~15x the
#  mean wait time, regardless of longer specified times)
THINK_MAX = 75
THINK_INTERVAL = 1

# Set SESSION_TIMEOUT > 0 to emulate session timeout for some sessions
# (with % of sessions timing out controlled by state transition values);
# set to 0 to end all sessions by logging out, instead
SESSION_TIMEOUT = 1

# Validatation directives - commenting out disables validation; results
# in non-compliant benchmark result (assigned value meaningless)
VALIDATE_TITLE = 1
VALIDATE_DYN_DATA = 1
VALIDATE_EMB_TXT = 1

# Time-based QOS criteria. The time limit (in milliseconds) below
# which response times meet the respective QOS
TIME_GOOD = 3000
TIME_TOLERABLE = 5000
TIME_GOOD_PCT = 95
TIME_TOL_PCT = 99
MIN_REQUESTS = 100

# Byte-rate-based QOS criteria. The number of bytes/sec that a
# request is expected to meet for each defined QOS
BYTE_RATE_GOOD = 99000
BYTE_RATE_TOLERABLE = 95000

ZIPF_ALPHA = 1.2

# RESULT_REF_SCORE is the reference value used to calculate the
# final benchmark result value (relative to this reference point)
RESULT_REF_SCORE = 162

# State transition probabilities
# If cumulative probability is less than 1 and time-outs are enabled, the
# remainder is timeout probability.
NUM_STATES = "7"

STATE_0 = "1,0.25,2,0.75"
STATE_1 = "3,0.5,1,0.5"
STATE_2 = "3,0.75,1,0.25"
STATE_3 = "4,0.80,2,0.20"
STATE_4 = "5,0.60,3,0.40"
STATE_5 = "6,0.50,4,0.20,3,0.05,2,0.05,1,0.10,0,0.10"
STATE_6 = "7,1.0"

NUM_PAGES = 7

# Page Files
PAGE_0_FILES = "4,6,7,8,14,17,23,24,26,27"
PAGE_1_FILES = "4,6,8,14,17,23,24,26,31"
PAGE_2_FILES = "4,6,7,8,14,17,23,24"
PAGE_3_FILES = "1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,28,29,30"
PAGE_4_FILES = "1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25"
PAGE_5_FILES = "1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25"
PAGE_6_FILES = ""

DYN_CMD_0_EXPECTED_PCT = 8.1072
DYN_CMD_1_EXPECTED_PCT = 12.6125
DYN_CMD_2_EXPECTED_PCT = 11.7088
DYN_CMD_3_EXPECTED_PCT = 24.7746
DYN_CMD_4_EXPECTED_PCT = 22.5237
DYN_CMD_5_EXPECTED_PCT = 13.5135
DYN_CMD_6_EXPECTED_PCT = 6.7597

APPEND_SZ_0 = 51700
APPEND_SZ_1 = 18000
APPEND_SZ_2 = 27500
APPEND_SZ_3 = 52700
APPEND_SZ_4 = 94500
APPEND_SZ_5 = 113300
APPEND_SZ_6 = -1

# Image File Details
# File# = "File Name,File Size, Response"
NUM_FILES = 31
FILE_1_DATA  = "aaa,30,0.66"
FILE_2_DATA  = "ccc,30,0.66"
FILE_3_DATA  = "___,810,0.66"
FILE_4_DATA  = "bar,60,0.8"
FILE_5_DATA  = "blue_arrow_right,50,0.66"
FILE_6_DATA  = "blue_arrow_top,50,0.8"
FILE_7_DATA  = "content_action,270,0.75"
FILE_8_DATA  = "content_arrow,130,0.8"
FILE_9_DATA  = "email,110,0.66"
FILE_10_DATA = "flattab_nl,80,0.66"
FILE_11_DATA = "flattab_nr,90,0.66"
FILE_12_DATA = "flattab_sl,90,0.66"
FILE_13_DATA = "flattab_sr,90,0.66"
FILE_14_DATA = "global,4180,0.8"
FILE_15_DATA = "help,700,0.66"
FILE_16_DATA = "H_D,1830,0.66"
FILE_17_DATA = "masthead_transparent,1510,0.8"
FILE_18_DATA = "masthead_global,110,0.66"
FILE_19_DATA = "masthead_local_sep,70,0.66"
FILE_20_DATA = "masthead_subnavsep,60,0.66"
FILE_21_DATA = "nav_q,250,0.66"
FILE_22_DATA = "print,360,0.66"
FILE_23_DATA = "spacer,40,0.8"
FILE_24_DATA = "template_javascripts,5670,0.8"
FILE_25_DATA = "us,80,0.66"
FILE_26_DATA = "note,980,0.5"
FILE_27_DATA = "h_product_selection,1920,0.0"
FILE_28_DATA = "button-1,240,0.0"
FILE_29_DATA = "button-2,260,0.0"
FILE_30_DATA = "button-3,260,0.0"
FILE_31_DATA = "H_Service_Tag_Unkown,2170,0.0"

PAGE_0_TITLE = "SPECweb2005: Support Home"
PAGE_1_TITLE = "SPECweb2005: Support Search Results for"
PAGE_2_TITLE = "SPECweb2005: Support Category"
PAGE_3_TITLE = "SPECweb2005: Support Product"
PAGE_4_TITLE = "SPECweb2005: Support Product"
PAGE_5_TITLE = "SPECweb2005: Support File"
PAGE_6_TITLE = ""

NUM_LANGUAGE_LIST = 30
NUM_OS_LIST = 10
NUM_DOWNLOAD_LIST = 20

# MARKER_FREQ is the interval between special characters in the
# wafgen-generated text files. It must be equal to the MARKERFREQUENCY
# value used by Wafgen in creating these files.
MARKER_FREQ = 4096

# how many digits the download directory name is (i.e. dir0000000001)
DOWNLOAD_NUM_LEN = 10

# Directory scaling, this must match the wafgen rc file
DIRSCALING = "0.25"

# Number of downloads in each directory; determined by # classes and
files in wafgen file
DOWNLOADS_PER_DIR = "16"

NUM_CLASSES = 6
CLASS_0_DIST = 0.1366
CLASS_1_DIST = 0.1261
CLASS_2_DIST = 0.2840
CLASS_3_DIST = 0.2232
CLASS_4_DIST = 0.1250
CLASS_5_DIST = 0.1051

CLASS_0_FILE_DIST = "0.273, 0.091, 0.165, 0.186, 0.285"
CLASS_1_FILE_DIST = "0.579, 0.178, 0.243"
CLASS_2_FILE_DIST = "0.275, 0.170, 0.170, 0.385"
CLASS_3_FILE_DIST = "0.667, 0.333"
CLASS_4_FILE_DIST = "1.000"
CLASS_5_FILE_DIST = "1.000"

CLASS_0_FILE_SIZE = "104858, 104858"
CLASS_1_FILE_SIZE = "629146, 125829"
CLASS_2_FILE_SIZE = "1048576, 492831"
CLASS_3_FILE_SIZE = "4194304, 1352663"
CLASS_4_FILE_SIZE = "9992929, 0"
CLASS_5_FILE_SIZE = "37748736, 0"

        </xsl:text>


    </xsl:template>

</xsl:stylesheet>
