<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
    <xsl:output method="text"/>
    <xsl:param name="outputDir">oDir</xsl:param>
    <xsl:template match="/specweb2005Benchmark">
        <xsl:text>DYNAMIC_SCRIPT = </xsl:text> <xsl:value-of select="bankingParameters/bankingWorkloadDetail/dynScript"/>

        <xsl:text>&#10;&#10;INIT_SCRIPT = </xsl:text> <xsl:value-of select="bankingParameters/bankingWorkloadDetail/initScript"/>

        <xsl:text>&#10;&#10;IMG_PATH = </xsl:text> <xsl:value-of select="bankingParameters/bankingWorkloadDetail/imgPath"/>

        <xsl:text>&#10;&#10;DYN_SCRIPT_PATH = </xsl:text> <xsl:value-of select="bankingParameters/bankingWorkloadDetail/dynScriptPath"/>

        <xsl:text>&#10;&#10;PADDING_DIR = </xsl:text> <xsl:value-of select="bankingParameters/bankingWorkloadDetail/paddingDir"/>

        <xsl:text>&#10;&#10; CHECK_IMAGE_DIR = </xsl:text> <xsl:value-of select="bankingParameters/bankingWorkloadDetail/checkImageDir"/>
        <xsl:text>&#10;CHECK_IMAGE_SUBDIRS = </xsl:text> <xsl:value-of select="bankingParameters/bankingWorkloadDetail/checkImageSubdirs"/>
        <xsl:text>&#10;BASE_SUBDIR_NAME = </xsl:text> <xsl:value-of select="bankingParameters/bankingWorkloadDetail/baseSubdir"/>
        <xsl:text>&#10;FIRST_SUBDIR_NUM = </xsl:text> <xsl:value-of select="bankingParameters/bankingWorkloadDetail/firstSubdirNum"/>
        <xsl:text>&#10;USER_NUM_LEN = </xsl:text> <xsl:value-of select="bankingParameters/bankingWorkloadDetail/userNumLen"/>
        <xsl:text>&#10;CHECK_NUM_LEN = </xsl:text> <xsl:value-of select="bankingParameters/bankingWorkloadDetail/checkNumLen"/>
        <xsl:text>&#10;CHECK_IMAGE_REL_PATH = </xsl:text> <xsl:value-of select="bankingParameters/bankingWorkloadDetail/checkImageRelPath"/>

        <xsl:text>&#10;&#10;DYN_SCRIPT_0 = </xsl:text> <xsl:value-of select="bankingParameters/bankingWorkloadDetail/dynScript0"/>
        <xsl:text>&#10;DYN_SCRIPT_1 = </xsl:text> <xsl:value-of select="bankingParameters/bankingWorkloadDetail/dynScript1"/>
        <xsl:text>&#10;DYN_SCRIPT_2 = </xsl:text> <xsl:value-of select="bankingParameters/bankingWorkloadDetail/dynScript2"/>
        <xsl:text>&#10;DYN_SCRIPT_3 = </xsl:text> <xsl:value-of select="bankingParameters/bankingWorkloadDetail/dynScript3"/>
        <xsl:text>&#10;DYN_SCRIPT_4 = </xsl:text> <xsl:value-of select="bankingParameters/bankingWorkloadDetail/dynScript4"/>
        <xsl:text>&#10;DYN_SCRIPT_5 = </xsl:text> <xsl:value-of select="bankingParameters/bankingWorkloadDetail/dynScript5"/>
        <xsl:text>&#10;DYN_SCRIPT_6 = </xsl:text> <xsl:value-of select="bankingParameters/bankingWorkloadDetail/dynScript6"/>
        <xsl:text>&#10;DYN_SCRIPT_7 = </xsl:text> <xsl:value-of select="bankingParameters/bankingWorkloadDetail/dynScript7"/>
        <xsl:text>&#10;DYN_SCRIPT_8 = </xsl:text> <xsl:value-of select="bankingParameters/bankingWorkloadDetail/dynScript8"/>
        <xsl:text>&#10;DYN_SCRIPT_9 = </xsl:text> <xsl:value-of select="bankingParameters/bankingWorkloadDetail/dynScript9"/>
        <xsl:text>&#10;DYN_SCRIPT_10 = </xsl:text> <xsl:value-of select="bankingParameters/bankingWorkloadDetail/dynScript10"/>
        <xsl:text>&#10;DYN_SCRIPT_11 = </xsl:text> <xsl:value-of select="bankingParameters/bankingWorkloadDetail/dynScript11"/>
        <xsl:text>&#10;DYN_SCRIPT_12 = </xsl:text> <xsl:value-of select="bankingParameters/bankingWorkloadDetail/dynScript12"/>
        <xsl:text>&#10;DYN_SCRIPT_13 = </xsl:text> <xsl:value-of select="bankingParameters/bankingWorkloadDetail/dynScript13"/>
        <xsl:text>&#10;DYN_SCRIPT_14 = </xsl:text> <xsl:value-of select="bankingParameters/bankingWorkloadDetail/dynScript14"/>
        <xsl:text>&#10;DYN_SCRIPT_15 = </xsl:text> <xsl:value-of select="bankingParameters/bankingWorkloadDetail/dynScript15"/>
        <xsl:text>&#10;DYN_SCRIPT_16 = </xsl:text> <xsl:value-of select="bankingParameters/bankingWorkloadDetail/dynScript16"/>

        <xsl:text>&#10;&#10;SERVER_CERTIFICATE_FILE = </xsl:text> <xsl:value-of select="bankingParameters/bankingWorkloadDetail/serverCertFile"/>
        <xsl:text>&#10;&#10;SERVER_CERTIFICATE_PWD = </xsl:text> <xsl:value-of select="bankingParameters/bankingWorkloadDetail/serverCertPwd"/>

        <xsl:text>&#10;&#10;LG_BUF_SIZE = </xsl:text> <xsl:value-of select="bankingParameters/bankingWorkloadDetail/bufSize"/>


        <xsl:text>
########################################################################
# IMG_SERVER_SSL_PORT is the port used with a separate image server to
# handle SSL requests (non-SSL image requests are handled by the port
# specified by IMG_SERVER_PORT in Test.config)
# IMG_SERVER_SSL_PORT = 443

# Dynamic request commands for each page -- used only if a *single*
# script file (specified in DYNAMIC_PATH) is used for all pages
DYN_CMD_0 = "login"
DYN_CMD_1 = "account_summary"
DYN_CMD_2 = "check_detail_html"
DYN_CMD_3 = "bill_pay"
DYN_CMD_4 = "add_payee"
DYN_CMD_5 = "payee_info"
DYN_CMD_6 = "quick_pay"
DYN_CMD_7 = "billpay_status"
DYN_CMD_8 = "chg_profile"
DYN_CMD_9 = "post_profile"
DYN_CMD_10 = "req_checks"
DYN_CMD_11 = "post_chk_order"
DYN_CMD_12 = "req_xfer_form"
DYN_CMD_13 = "post_fund_xfer"
DYN_CMD_14 = "logout"
DYN_CMD_15 = "check_image"
DYN_CMD_16 = "check_image"

########################################################################
# Fixed workload properties
# Changing these values will result in an invalid benchmark run
########################################################################

# Mean wait time (in seconds) between requests
THINK_TIME = 10

# Measured value for percentage of requests using think time
THINK_TIME_REQ_PCT = 61.58

# Max wait time (in seconds) between requests; should be evenly
# divisible by THINK_INTERVAL
# (note: effectively, max think time will not be more than ~15x the
#  mean wait time, regardless of longer specified times)
THINK_MAX = 150
THINK_INTERVAL = 2

# Set SESSION_TIMEOUT > 0 to emulate session timeout for some sessions
# (with % of sessions timing out controlled by state transition values);
# set to 0 to end all sessions by logging out, instead
SESSION_TIMEOUT = 1

# For debugging purposes only; uses non-SSL connections if set to 0
# or commented out
</xsl:text>

<xsl:text>&#10;&#10;USE_SSL = </xsl:text> <xsl:value-of select="bankingParameters/bankingWorkloadDetail/useSSL"/>

<xsl:text>
# Validatation directives - commenting out or setting to 0 disables
# validation; results in non-compliant benchmark result
VALIDATE_TITLE = 1
VALIDATE_DYN_DATA = 1
VALIDATE_EMB_TXT = 1

# QOS criteria. The time limit (in milliseconds) below which these
# values apply to request response times
TIME_GOOD = 2000
TIME_TOLERABLE = 4000
TIME_GOOD_PCT = 95
TIME_TOL_PCT = 99
MIN_REQUESTS = 100

# RESULT_REF_SCORE is the reference value used to calculate the
# final benchmark result value (relative to this reference point)
RESULT_REF_SCORE = 120

# CLIENT_SESSION_USER_IDS is the number of different users each client
# thread can emulate during the benchmark, emulating only one of these
# users during any given session. Total emulated users would be equal
# to CLIENT_SESSION_USER_IDS * SIMULTANEOUS_SESSIONS, and must not
# exceed 100,000,000.

CLIENT_SESSION_USER_IDS = 50
CHECK_IMAGES_PER_USER = 10

# Check image base and incremental length values
CHECK_BASE_LEN_BACK = 5458
CHECK_LEN_INCR_BACK = 334
CHECK_BASE_LEN_FRONT = 10757
CHECK_LEN_INCR_FRONT = 511

# MARKER_FREQ is the interval between special characters in the
# wafgen-generated text files. It must be equal to the MARKERFREQUENCY
# value used by Wafgen in creating these files.
MARKER_FREQ = 4096

# State transition probabilities
# If cumulative probability is less than 1 and time-outs are enabled, the
# remainder is timeout probability, otherwise the remainder is assigned to
# (or added to) the logout probability (State 14)
NUM_STATES = "17"

STATE_0 = "1,0.32,3,0.32,12,0.08,14,0.08"
STATE_1 = "2,0.56,10,0.08,8,0.08,14,0.08"
STATE_2 = "15,1.0"
STATE_3 = "4,0.08,6,0.48,7,0.16,14,0.08"
STATE_4 = "5,0.72,14,0.08"
STATE_5 = "3,0.72,14,0.08"
STATE_6 = "3,0.72,14,0.08"
STATE_7 = "3,0.72,14,0.08"
STATE_8 = "9,0.72,14,0.08"
STATE_9 = "1,0.72,14,0.08"
STATE_10 = "11,0.72,14,0.08"
STATE_11 = "1,0.72,14,0.08"
STATE_12 = "13,0.72,14,0.08"
STATE_13 = "1,0.72,14,0.08"
STATE_15 = "16,1.0"
STATE_16 = "1,0.72,14,0.08"

NUM_PAGES = 17

# Page Files
PAGE_0_FILES = "5,1,2,3,4,6,7,8"
PAGE_1_FILES = "1,2,3,4,7,8,9,10,11,12,13,14,15,16"
PAGE_2_FILES = "1,2,3,4,7,8,9,10,11,12,13,14,15,16,18,19,20,32"
PAGE_3_FILES = "1,2,3,4,7,8,9,15,17,21,22,27,28,33"
PAGE_4_FILES = "1,2,3,4,7,8,9,14,15,17,21,22,27"
PAGE_5_FILES = "1,2,3,4,7,8,9,14,15,17,21,22,23,27,34"
PAGE_6_FILES = "1,2,3,4,7,8,9,14,15,17,21,22,24,25,26,27,33"
PAGE_7_FILES = "1,2,4,7,8,9,11,15,17,21,22,26,33"
PAGE_8_FILES = "1,2,3,4,7,8,9,10,14,15,16,17,28,29"
PAGE_9_FILES = "1,2,3,4,7,8,9,10,14,15,16,17"
# PAGE_10_FILES = "1,2,3,4,7,8,9,10,14,15,16,17,30,31,35,36,37,38,39,40,41,42,43,44"
PAGE_10_FILES = "1,2,3,4,7,8,9,10,14,15,16,17,30,31,35,36,37,38,39"
PAGE_11_FILES = "1,2,3,4,7,8,9,10,11,14,15,16,17"
PAGE_12_FILES = "1,2,3,4,7,8,9,10,11,14,15,16,17"
PAGE_13_FILES = "1,2,3,4,7,8,9,10,11,14,15,16,17"
PAGE_14_FILES = "5,1,2,3,4,6,7,8"

DYN_CMD_0_EXPECTED_PCT = 21.5280
DYN_CMD_1_EXPECTED_PCT = 15.1146
DYN_CMD_2_EXPECTED_PCT = 8.4470
DYN_CMD_3_EXPECTED_PCT = 13.8908
DYN_CMD_4_EXPECTED_PCT = 1.1163
DYN_CMD_5_EXPECTED_PCT = 0.8043
DYN_CMD_6_EXPECTED_PCT = 6.6746
DYN_CMD_7_EXPECTED_PCT = 2.2281
DYN_CMD_8_EXPECTED_PCT = 1.2165
DYN_CMD_9_EXPECTED_PCT = 0.8761
DYN_CMD_10_EXPECTED_PCT = 1.2155
DYN_CMD_11_EXPECTED_PCT = 0.8784
DYN_CMD_12_EXPECTED_PCT = 1.7146
DYN_CMD_13_EXPECTED_PCT = 1.2415
DYN_CMD_14_EXPECTED_PCT = 6.1597
DYN_CMD_15_EXPECTED_PCT = 16.8940
DYN_CMD_16_EXPECTED_PCT = 16.8940

APPEND_SZ_0 = 4000
APPEND_SZ_1 = 17000
APPEND_SZ_2 = 11000
APPEND_SZ_3 = 15000
APPEND_SZ_4 = 18000
APPEND_SZ_5 = 34000
APPEND_SZ_6 = 22000
APPEND_SZ_7 = 24000
APPEND_SZ_8 = 32000
APPEND_SZ_9 = 29000
APPEND_SZ_10 = 21000
APPEND_SZ_11 = 25000
APPEND_SZ_12 = 13000
APPEND_SZ_13 = 16000
APPEND_SZ_14 = 46000


# Image File Details
NUM_FILES = 44
FILE_1_DATA = "image1,806,0.5"
FILE_2_DATA = "image2,246,0.4"
FILE_3_DATA = "rectangle,43,0.4"
FILE_4_DATA = "logo,1140,0.5"
FILE_5_DATA = "fornewusers,36789,0.33"
FILE_6_DATA = "continue,764,0.33"
FILE_7_DATA = "secure,1522,0.7"
FILE_8_DATA = "house,858,0.6"
FILE_9_DATA = "mail,216,0.3"
FILE_10_DATA = "help,205,0.2"
FILE_11_DATA = "signoff,316,0.1"
FILE_12_DATA = "tab,52,0"
FILE_13_DATA = "account,1285,0"
FILE_14_DATA = "billpay,694,0.1"
FILE_15_DATA = "transfer,676,0.5"
FILE_16_DATA = "customerservice,778,0.1"
FILE_17_DATA = "account1,488,0.3"
FILE_18_DATA = "downloadbutton,764,0"
FILE_19_DATA = "viewback,731,0"
FILE_20_DATA = "returnlost,1059,0"
FILE_21_DATA = "payees,224,0"
FILE_22_DATA = "ebills,208,0.2"
FILE_23_DATA = "eimage,952,0"
FILE_24_DATA = "addbill,646,0"
FILE_25_DATA = "quickpay,353,0"
FILE_26_DATA = "displaypayment,915,0"
FILE_27_DATA = "payment,234,0"
FILE_28_DATA = "changenickname,1043,0"
FILE_29_DATA = "notchangenickname,1178,0"
FILE_30_DATA = "ordercheckcopy,1024,0"
FILE_31_DATA = "dontorderchkcopy,1170,0"
FILE_32_DATA = "sort,102,0.5"
FILE_33_DATA = "billpayopen,603,0.1"
FILE_34_DATA = "addnewpayee,943,0"
FILE_35_DATA = "check_01.jpg,11838,0"
FILE_36_DATA = "check_02.jpg,6481,0"
FILE_37_DATA = "check_03.jpg,12836,0"
FILE_38_DATA = "check_04.jpg,10868,0"
FILE_39_DATA = "check_05.jpg,11405,0"
FILE_40_DATA = "check_06.jpg,5792,0"
FILE_41_DATA = "check_07.jpg,10905,0"
FILE_42_DATA = "check_08.jpg,5458,0"
FILE_43_DATA = "check_09.jpg,11731,0"
FILE_44_DATA = "check_10.jpg,10757,0"

PAGE_0_TITLE = "SPECweb2005: Welcome"
PAGE_1_TITLE = "SPECweb2005: Account Summary"
PAGE_2_TITLE = "SPECweb2005: Check Detail Output"
PAGE_3_TITLE = "SPECweb2005: Bill Pay"
PAGE_4_TITLE = "SPECweb2005: Add Payee"
PAGE_5_TITLE = "SPECweb2005: Post Payee"
PAGE_6_TITLE = "SPECweb2005: Quick Pay Result"
PAGE_7_TITLE = "SPECweb2005: Bill Pay Status"
PAGE_8_TITLE = "SPECweb2005: Profile"
PAGE_9_TITLE = "SPECweb2005: Profile Changed"
PAGE_10_TITLE = "SPECweb2005: Order Checks"
PAGE_11_TITLE = "SPECweb2005: Place Check Order Result"
PAGE_12_TITLE = "SPECweb2005: Transfer Money"
PAGE_13_TITLE = "SPECweb2005: Transfer Result"
PAGE_14_TITLE = "SPECweb2005: Logout"
PAGE_15_TITLE = "SPECweb2005: Login";


# Add Payee List Data

NUM_PAYEES = 10

PAYEE_0_TYPE = "Electric"
PAYEE_1_TYPE = "Mortgage"
PAYEE_2_TYPE = "CreditCard"
PAYEE_3_TYPE = "Oil"
PAYEE_4_TYPE = "Cable"
PAYEE_5_TYPE = "Insurance"
PAYEE_6_TYPE = "Telephone"
PAYEE_7_TYPE = "Wireless"
PAYEE_8_TYPE = "Auto"
PAYEE_9_TYPE = "IRS"

PAYEE_0_NAME = "Electric Co."
PAYEE_1_NAME = "Mortgage Inc."
PAYEE_2_NAME = "CreditCard"
PAYEE_3_NAME = "Oil Co."
PAYEE_4_NAME = "Cable"
PAYEE_5_NAME = "Insurance Inc."
PAYEE_6_NAME = "Telephone Co."
PAYEE_7_NAME = "Wireless Inc."
PAYEE_8_NAME = "Auto Inc."
PAYEE_9_NAME = "IRS"

PAYEE_0_STREET = "Aspen Rd"
PAYEE_1_STREET = "Birch Ln"
PAYEE_2_STREET = "CloudTree Ave"
PAYEE_3_STREET = "Dogwood Dr"
PAYEE_4_STREET = "Elm St"
PAYEE_5_STREET = "Heath Way"
PAYEE_6_STREET = "Linden Blvd"
PAYEE_7_STREET = "Magnolia Ave"
PAYEE_8_STREET = "Olive Cirle"
PAYEE_9_STREET = "Palm way"

PAYEE_0_CITY = "Northford"
PAYEE_1_CITY = "Easton"
PAYEE_2_CITY = "Sudbury"
PAYEE_3_CITY = "Westville"
PAYEE_4_CITY = "Middletown"
PAYEE_5_CITY = "Portmouth"
PAYEE_6_CITY = "Bridgewater"
PAYEE_7_CITY = "Littleton"
PAYEE_8_CITY = "Wayland"
PAYEE_9_CITY = "Upton"

PAYEE_0_STATE = "PA"
PAYEE_1_STATE = "OH"
PAYEE_2_STATE = "NJ"
PAYEE_3_STATE = "MD"
PAYEE_4_STATE = "DE"
PAYEE_5_STATE = "VA"
PAYEE_6_STATE = "NC"
PAYEE_7_STATE = "SC"
PAYEE_8_STATE = "TN"
PAYEE_9_STATE = "WV"

PAYEE_0_ZIP = "01812"
PAYEE_1_ZIP = "14112"
PAYEE_2_ZIP = "13821"
PAYEE_3_ZIP = "02012"
PAYEE_4_ZIP = "01934"
PAYEE_5_ZIP = "01732"
PAYEE_6_ZIP = "01431"
PAYEE_7_ZIP = "03821"
PAYEE_8_ZIP = "05014"
PAYEE_9_ZIP = "01967"

PAYEE_0_TEL = "123-234-3456"
PAYEE_1_TEL = "223-334-4456"
PAYEE_2_TEL = "333-445-5678"
PAYEE_3_TEL = "414-516-7271"
PAYEE_4_TEL = "555-221-0987"
PAYEE_5_TEL = "800-888-1223"
PAYEE_6_TEL = "866-912-8765"
PAYEE_7_TEL = "777-667-1256"
PAYEE_8_TEL = "929-877-4929"
PAYEE_9_TEL = "877-392-2855"


PAYMT_0_AMT = "100.00"
PAYMT_1_AMT = "1273.42"
PAYMT_2_AMT = "20.00"
PAYMT_3_AMT = "120.00"
PAYMT_4_AMT = "40.00"
PAYMT_5_AMT = "179.28"
PAYMT_6_AMT = "29.00"
PAYMT_7_AMT = "39.99"
PAYMT_8_AMT = "207.00"
PAYMT_9_AMT = "250.00"

        </xsl:text>


    </xsl:template>

</xsl:stylesheet>
