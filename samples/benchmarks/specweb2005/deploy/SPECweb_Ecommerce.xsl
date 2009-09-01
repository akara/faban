<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
    <xsl:output method="text"/>
    <xsl:param name="outputDir">oDir</xsl:param>
    <xsl:template match="/specweb2005Benchmark">
        <xsl:text>DYNAMIC_SCRIPT = </xsl:text> <xsl:value-of select="ecommerceParameters/ecommerceWorkloadDetail/dynScript"/>

        <xsl:text>&#10;&#10;INIT_SCRIPT = </xsl:text> <xsl:value-of select="ecommerceParameters/ecommerceWorkloadDetail/initScript"/>

        <xsl:text>&#10;&#10;IMG_PATH = </xsl:text> <xsl:value-of select="ecommerceParameters/ecommerceWorkloadDetail/imgPath"/>

        <xsl:text>&#10;&#10;DYN_SCRIPT_PATH = </xsl:text> <xsl:value-of select="ecommerceParameters/ecommerceWorkloadDetail/dynScriptPath"/>

        <xsl:text>&#10;&#10;PADDING_DIR = </xsl:text> <xsl:value-of select="ecommerceParameters/ecommerceWorkloadDetail/paddingDir"/>

        <xsl:text>&#10;&#10;PRODUCTLINE_NUM_LEN = </xsl:text> <xsl:value-of select="ecommerceParameters/ecommerceWorkloadDetail/productlineNumLen"/>
        <xsl:text>&#10;PRODUCT_IMAGE_REL_PATH = </xsl:text> <xsl:value-of select="ecommerceParameters/ecommerceWorkloadDetail/productImageRelPath"/>
        <xsl:text>&#10;DIRSCALING = </xsl:text> <xsl:value-of select="ecommerceParameters/ecommerceWorkloadDetail/dirScaling"/>
        <xsl:text>&#10;PRODUCTS_PER_DIR = </xsl:text> <xsl:value-of select="ecommerceParameters/ecommerceWorkloadDetail/productsPerDir"/>

        <xsl:text>&#10;&#10;PRODUCT_IMAGE_SUBDIRS = </xsl:text> <xsl:value-of select="ecommerceParameters/ecommerceWorkloadDetail/productImageSubdirs"/>
        <xsl:text>&#10;BASE_SUBDIR_NAME = </xsl:text> <xsl:value-of select="ecommerceParameters/ecommerceWorkloadDetail/baseSubdir"/>
        <xsl:text>&#10;FIRST_SUBDIR_NUM = </xsl:text> <xsl:value-of select="ecommerceParameters/ecommerceWorkloadDetail/firstSubdirNum"/>

        <xsl:text>&#10;&#10;DYN_SCRIPT_0 = </xsl:text> <xsl:value-of select="ecommerceParameters/ecommerceWorkloadDetail/dynScript0"/>
        <xsl:text>&#10;DYN_SCRIPT_1 = </xsl:text> <xsl:value-of select="ecommerceParameters/ecommerceWorkloadDetail/dynScript1"/>
        <xsl:text>&#10;DYN_SCRIPT_2 = </xsl:text> <xsl:value-of select="ecommerceParameters/ecommerceWorkloadDetail/dynScript2"/>
        <xsl:text>&#10;DYN_SCRIPT_3 = </xsl:text> <xsl:value-of select="ecommerceParameters/ecommerceWorkloadDetail/dynScript3"/>
        <xsl:text>&#10;DYN_SCRIPT_4 = </xsl:text> <xsl:value-of select="ecommerceParameters/ecommerceWorkloadDetail/dynScript4"/>
        <xsl:text>&#10;DYN_SCRIPT_5 = </xsl:text> <xsl:value-of select="ecommerceParameters/ecommerceWorkloadDetail/dynScript5"/>
        <xsl:text>&#10;DYN_SCRIPT_6 = </xsl:text> <xsl:value-of select="ecommerceParameters/ecommerceWorkloadDetail/dynScript6"/>
        <xsl:text>&#10;DYN_SCRIPT_7 = </xsl:text> <xsl:value-of select="ecommerceParameters/ecommerceWorkloadDetail/dynScript7"/>
        <xsl:text>&#10;DYN_SCRIPT_8 = </xsl:text> <xsl:value-of select="ecommerceParameters/ecommerceWorkloadDetail/dynScript8"/>
        <xsl:text>&#10;DYN_SCRIPT_9 = </xsl:text> <xsl:value-of select="ecommerceParameters/ecommerceWorkloadDetail/dynScript9"/>
        <xsl:text>&#10;DYN_SCRIPT_10 = </xsl:text> <xsl:value-of select="ecommerceParameters/ecommerceWorkloadDetail/dynScript10"/>
        <xsl:text>&#10;DYN_SCRIPT_11 = </xsl:text> <xsl:value-of select="ecommerceParameters/ecommerceWorkloadDetail/dynScript11"/>
        <xsl:text>&#10;DYN_SCRIPT_12 = </xsl:text> <xsl:value-of select="ecommerceParameters/ecommerceWorkloadDetail/dynScript12"/>
        <xsl:text>&#10;DYN_SCRIPT_13 = </xsl:text> <xsl:value-of select="ecommerceParameters/ecommerceWorkloadDetail/dynScript13"/>

        <xsl:text>&#10;&#10;SERVER_CERTIFICATE_FILE = </xsl:text> <xsl:value-of select="ecommerceParameters/ecommerceWorkloadDetail/serverCertFile"/>
        <xsl:text>&#10;&#10;SERVER_CERTIFICATE_PWD = </xsl:text> <xsl:value-of select="ecommerceParameters/ecommerceWorkloadDetail/serverCertPwd"/>

        <xsl:text>&#10;&#10;LG_BUF_SIZE = </xsl:text> <xsl:value-of select="ecommerceParameters/ecommerceWorkloadDetail/bufSize"/>


        <xsl:text>
########################################################################
# IMG_SERVER_SSL_PORT is the port used with a separate image server to
# handle SSL requests (non-SSL image requests are handled by the port
# specified by IMG_SERVER_PORT in Test.config)
# IMG_SERVER_SSL_PORT = 443

# Dynamic request commands for each page -- used only if a *single*
# script file (specified in DYNAMIC_PATH) is used for all pages
DYN_CMD_0 = "index"
DYN_CMD_1 = "search"
DYN_CMD_2 = "browse"
DYN_CMD_3 = "browse_productline"
DYN_CMD_4 = "productdetail"
DYN_CMD_5 = "customize1"
DYN_CMD_6 = "customize2"
DYN_CMD_7 = "customize3"
DYN_CMD_8 = "cart"
DYN_CMD_9 = "login"
DYN_CMD_10 = "shipping"
DYN_CMD_11 = "billing"
DYN_CMD_12 = "confirm"


########################################################################
# Fixed workload properties
# Changing these values will result in an invalid benchmark run
########################################################################

# Mean wait time (in seconds) between requests
THINK_TIME = 10

# Measured value for percentage of requests using think time
THINK_TIME_REQ_PCT = 91.94

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

# For debugging purposes only; comment out or set to 0 to use non-SSL
# connections. Benchmark result will be non-compliant

</xsl:text>

<xsl:text>&#10;&#10;USE_SSL = </xsl:text> <xsl:value-of select="ecommerceParameters/ecommerceWorkloadDetail/useSSL"/>

<xsl:text>

# Validatation directives - commenting out disables validation; results
# in non-compliant benchmark result (assigned value meaningless)
VALIDATE_TITLE = 1
VALIDATE_DYN_DATA = 1
VALIDATE_EMB_TXT = 1


# QOS criteria. The time limit (in milliseconds) below which these
# values apply to request response times
TIME_GOOD = 3000
TIME_TOLERABLE = 5000
TIME_GOOD_PCT = 95
TIME_TOL_PCT = 99
MIN_REQUESTS = 100

# RESULT_REF_SCORE is the reference value used to calculate the
# final benchmark result value (relative to this reference point)
RESULT_REF_SCORE = 114

# CLIENT_SESSION_USER_IDS is the number of different users each client
# thread can emulate during the benchmark, emulating only one of these
# users during any given session. Total emulated users would be equal
# to CLIENT_SESSION_USER_IDS * SIMULTANEOUS_SESSIONS, and must not
# exceed 100,000,000.

CLIENT_SESSION_USER_IDS = 10

# MARKER_FREQ is the interval between special characters in the
# wafgen-generated text files. It must be equal to the MARKERFREQUENCY
# value used by Wafgen in creating these files.
MARKER_FREQ = 4096

#PRODUCT IMAGE SIZE START AT
PRD_IMG_SIZE_0 = 3521
PRD_IMG_SIZE_1 = 6710
PRD_IMG_SIZE_2 = 5327
#INCREMENT
PRD_IMG_INCR_0 = 1586
PRD_IMG_INCR_1 = 3590
PRD_IMG_INCR_2 = 3911
#INCREMENT
PRD_IMG_CNT_0 = 10
PRD_IMG_CNT_1 = 10
PRD_IMG_CNT_2 = 10

# State transition probabilities
# If cumulative probability is less than 1 and time-outs are enabled, the
# remainder is timeout probability.
# Last state in STATE MUST be the exit state
NUM_STATES = "14"

#MUST BE IN THE QUOTES, There must be a space before the "="
#STATE_i = "NEXT_STATE0,PROB_NEXT_STATE0:NEXT_STATE1,PROB_NEXT_STATE1"
STATE_0 ="0,0.2:1,0.15:2,0.6:13,0.05"
STATE_1 ="0,0.05:1,0.7:2,0.2:13,0.05"
STATE_2 ="0,0.1:2,0.1:3,0.75:13,0.05"
STATE_3 ="0,0.05:2,0.1:4,0.8:13,0.05"
STATE_4 ="0,0.05:2,0.05:3,0.15:5,0.7:13,0.05"
STATE_5 ="5,0.45:6,0.5:13,0.05"
STATE_6 ="5,0.1:6,0.3:7,0.55:13,0.05"
STATE_7 ="6,0.1:7,0.2:8,0.65:13,0.05"
STATE_8 ="8,0.25:9,0.7:13,0.05"
STATE_9 ="9,0.5:10,0.45:13,0.05"
STATE_10 ="11,0.95:13,0.05"
STATE_11 ="12,0.95:13,0.05"
STATE_12 ="12,0.75:13,0.25"
STATE_13 ="13,1"

# Page Files
# State_0       PAGE_0: index
# State_1       PAGE_1: search
# State_2       PAGE_2: browse
# State_3       PAGE_3: browse product line
# State_4       PAGE_4: product detail
# State_5       PAGE_5: customize (stage 1)
# State_6       PAGE_6: customize (stage 2)
# State_7       PAGE_7: customize (stage 3)
# State_8       PAGE_8: cart
# State_9       PAGE_9: login
# State_10      PAGE_10: shipping
# State_11      PAGE_11: billing
# State_12      PAGE_12: confirm
# State_13  No page: exit

PAGE_0_FILES = "1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25"
PAGE_1_FILES = "153,154,155,156,157,158,159"
PAGE_2_FILES = "26,27,28,29,30,31,32,33,34,35,36,37,38,39,40,41,42,43,44,45,46,47,48,49,50"
PAGE_3_FILES = "51,52,53,54,55,56,57,58,59,60,61,62"
PAGE_4_FILES = "63,64,65,66,67,68,69,70,71,72"
PAGE_5_FILES = "73,74,75,76,77,78,79,80,81"
PAGE_6_FILES = "73,74,75,76,77,78,79,80,81"
PAGE_7_FILES = "73,74,75,76,77,78,79,80,81"
PAGE_8_FILES = "82,83,84,85,86,87,88,89,90,91,92,93,94,95,96,97,98,99,100,101,102,103,104,105,106,107,108,109,110,111,112,113,114,115,116,117,118,119,120,121"
PAGE_9_FILES = "122,123,124,125,126,127,128,129,130,131,132,133,134,135,136,137,138,139,140,141,142"PAGE_10_FILES = "143,144,145,146,147"
PAGE_11_FILES = "148,149,150,151,152"
PAGE_12_FILES = "160,161,162,163,164,165,166,167,168,169,170,171,172"

DYN_CMD_0_EXPECTED_PCT = 13.0834
DYN_CMD_1_EXPECTED_PCT = 6.5461
DYN_CMD_2_EXPECTED_PCT = 11.7532
DYN_CMD_3_EXPECTED_PCT = 10.0270
DYN_CMD_4_EXPECTED_PCT = 8.0178
DYN_CMD_5_EXPECTED_PCT = 16.9320
DYN_CMD_6_EXPECTED_PCT = 8.9483
DYN_CMD_7_EXPECTED_PCT = 6.1558
DYN_CMD_8_EXPECTED_PCT = 5.2968
DYN_CMD_9_EXPECTED_PCT = 3.7778
DYN_CMD_10_EXPECTED_PCT = 3.5525
DYN_CMD_11_EXPECTED_PCT = 3.3743
DYN_CMD_12_EXPECTED_PCT = 2.5350

#VALITEM_0 = "1"
#VALITEM_1 = "3"
#VALITEM_2 = "2"
#VALITEM_3 = "3"
#VALITEM_4 = "3"
#VALITEM_5 = "3"
#VALITEM_6 = "3"
#VALITEM_7 = "3"
#VALITEM_8 = "12"
#VALITEM_9 = "3"
#VALITEM_10 = "3"
#VALITEM_11 = "3"
#VALITEM_12 = "29"

APPEND_SZ_0 = 54000
APPEND_SZ_1 = 167000
APPEND_SZ_2 = 68000
APPEND_SZ_3 = 108000
APPEND_SZ_4 = 22000
APPEND_SZ_5 = 154000
APPEND_SZ_6 = 154000
APPEND_SZ_7 = 154000
APPEND_SZ_8 = 112000
APPEND_SZ_9 = 45000
APPEND_SZ_10 = 17000
APPEND_SZ_11 = 33000
APPEND_SZ_12 = 23000

# Image File Details
NUM_FILES = 172

FILE_1_DATA =   "homepage1,43,0.3"
FILE_2_DATA =   "homepage2,48,0.3"
FILE_3_DATA =   "homepage3,54,0.3"
FILE_4_DATA =   "homepage4,78,0.3"
FILE_5_DATA =   "homepage5,82,0.2"
FILE_6_DATA =   "homepage6,147,0.3"
FILE_7_DATA =   "homepage7,166,0.3"
FILE_8_DATA =   "homepage8,167,0.3"
FILE_9_DATA =   "homepage9,173,0.3"
FILE_10_DATA =   "homepage10,187,0.3"
FILE_11_DATA =   "homepage11,592,0.3"
FILE_12_DATA =   "homepage12,616,0.3"
FILE_13_DATA =   "homepage13,738,0.3"
FILE_14_DATA =   "homepage14,1022,0.3"
FILE_15_DATA =   "homepage15,1186,0.3"
FILE_16_DATA =   "homepage16,1259,0.3"
FILE_17_DATA =   "homepage17,1360,0.3"
FILE_18_DATA =   "homepage18,1550,0.3"
FILE_19_DATA =   "homepage19,1593,0.3"
FILE_20_DATA =   "homepage20,1761,0.3"
FILE_21_DATA =   "homepage21,1809,0.3"
FILE_22_DATA =   "homepage22,18346,0.2"
FILE_23_DATA =   "homepage23,19207,0.2"
FILE_24_DATA =   "homepage24,21838,0.2"
FILE_25_DATA =   "homepage25,22612,0.2"
FILE_26_DATA =   "browse1,443,0.3"
FILE_27_DATA =   "browse2,59,0.3"
FILE_28_DATA =   "browse3,62,0.3"
FILE_29_DATA =   "browse4,63,0.3"
FILE_30_DATA =   "browse5,71,0.3"
FILE_31_DATA =   "browse6,132,0.3"
FILE_32_DATA =   "browse7,175,0.3"
FILE_33_DATA =   "browse8,224,0.3"
FILE_34_DATA =   "browse9,243,0.3"
FILE_35_DATA =   "browse10,523,0.3"
FILE_36_DATA =   "browse11,917,0.3"
FILE_37_DATA =   "browse12,1167,0.3"
FILE_38_DATA =   "browse13,1347,0.3"
FILE_39_DATA =   "browse14,1402,0.3"
FILE_40_DATA =   "browse15,1480,0.3"
FILE_41_DATA =   "browse16,1504,0.3"
FILE_42_DATA =   "browse17,1621,0.3"
FILE_43_DATA =   "browse18,1738,0.3"
FILE_44_DATA =   "browse19,2208,0.2"
FILE_45_DATA =   "browse20,11918,0.2"
FILE_46_DATA =   "browse21,11972,0.2"
FILE_47_DATA =   "browse22,14408,0.2"
FILE_48_DATA =   "browse23,14525,0.2"
FILE_49_DATA =   "browse24,14748,0.2"
FILE_50_DATA =   "browse25,15750,0.2"
FILE_51_DATA =   "browse_productline1,49,0.3"
FILE_52_DATA =   "browse_productline2,72,0.3"
FILE_53_DATA =   "browse_productline3,185,0.3"
FILE_54_DATA =   "browse_productline4,1423,0.3"
FILE_55_DATA =   "browse_productline5,2176,0.3"
FILE_56_DATA =   "browse_productline6,3140,0.1"
FILE_57_DATA =   "browse_productline7,6828,0.1"
FILE_58_DATA =   "browse_productline8,8210,0.1"
FILE_59_DATA =   "browse_productline9,9461,0.1"
FILE_60_DATA =   "browse_productline10,10633,0.1"
FILE_61_DATA =   "browse_productline11,10774,0.1"
FILE_62_DATA =   "browse_productline12,11044,0.1"
FILE_63_DATA =   "productdetail1,43,0.3"
FILE_64_DATA =   "productdetail2,58,0.3"
FILE_65_DATA =   "productdetail3,71,0.3"
FILE_66_DATA =   "productdetail4,49,0.3"
FILE_67_DATA =   "productdetail5,121,0.3"
FILE_68_DATA =   "productdetail6,132,0.3"
FILE_69_DATA =   "productdetail7,187,0.3"
FILE_70_DATA =   "productdetail8,187,0.3"
FILE_71_DATA =   "productdetail9,2154,0.1"
FILE_72_DATA =   "productdetail10,3521,0.1"
FILE_73_DATA =   "customize1,43,0.3"
FILE_74_DATA =   "customize2,43,0.3"
FILE_75_DATA =   "customize3,49,0.3"
FILE_76_DATA =   "customize4,95,0.3"
FILE_77_DATA =   "customize5,114,0.3"
FILE_78_DATA =   "customize6,370,0.3"
FILE_79_DATA =   "customize7,1373,0.1"
FILE_80_DATA =   "customize8,1936,0.1"
FILE_81_DATA =   "customize9,1994,0.1"
FILE_82_DATA =   "cart1,43,0"
FILE_83_DATA =   "cart2,43,0"
FILE_84_DATA =   "cart3,43,0"
FILE_85_DATA =   "cart4,44,0"
FILE_86_DATA =   "cart5,44,0"
FILE_87_DATA =   "cart6,48,0"
FILE_88_DATA =   "cart7,50,0"
FILE_89_DATA =   "cart8,57,0"
FILE_90_DATA =   "cart9,61,0"
FILE_91_DATA =   "cart10,65,0"
FILE_92_DATA =   "cart11,82,0"
FILE_93_DATA =   "cart12,83,0"
FILE_94_DATA =   "cart13,91,0"
FILE_95_DATA =   "cart14,97,0"
FILE_96_DATA =   "cart15,98,0"
FILE_97_DATA =   "cart16,130,0"
FILE_98_DATA =   "cart17,136,0"
FILE_99_DATA =   "cart18,223,0"
FILE_100_DATA =   "cart19,243,0"
FILE_101_DATA =   "cart20,251,0"
FILE_102_DATA =   "cart21,274,0"
FILE_103_DATA =   "cart22,278,0"
FILE_104_DATA =   "cart23,280,0"
FILE_105_DATA =   "cart24,283,0"
FILE_106_DATA =   "cart25,319,0"
FILE_107_DATA =   "cart26,329,0"
FILE_108_DATA =   "cart27,362,0"
FILE_109_DATA =   "cart28,363,0"
FILE_110_DATA =   "cart29,385,0"
FILE_111_DATA =   "cart30,523,0"
FILE_112_DATA =   "cart31,621,0"
FILE_113_DATA =   "cart32,1848,0"
FILE_114_DATA =   "cart33,1980,0"
FILE_115_DATA =   "cart34,7894,0"
FILE_116_DATA =   "cart35,8240,0"
FILE_117_DATA =   "cart36,8255,0"
FILE_118_DATA =   "cart37,8484,0"
FILE_119_DATA =   "cart38,8886,0"
FILE_120_DATA =   "cart39,8914,0"
FILE_121_DATA =   "cart40,43,0"
FILE_122_DATA =   "checkout1,43,0"
FILE_123_DATA =   "checkout2,43,0"
FILE_124_DATA =   "checkout3,43,0"
FILE_125_DATA =   "checkout4,44,0"
FILE_126_DATA =   "checkout5,48,0"
FILE_127_DATA =   "checkout6,59,0"
FILE_128_DATA =   "checkout7,59,0"
FILE_129_DATA =   "checkout8,59,0"
FILE_130_DATA =   "checkout9,60,0"
FILE_131_DATA =   "checkout10,65,0"
FILE_132_DATA =   "checkout11,82,0"
FILE_133_DATA =   "checkout12,83,0"
FILE_134_DATA =   "checkout13,97,0"
FILE_135_DATA =   "checkout14,102,0"
FILE_136_DATA =   "checkout15,136,0"
FILE_137_DATA =   "checkout16,217,0"
FILE_138_DATA =   "checkout17,356,0"
FILE_139_DATA =   "checkout18,385,0"
FILE_140_DATA =   "checkout19,523,0"
FILE_141_DATA =   "checkout20,1648,0"
FILE_142_DATA =   "checkout21,1913,0"
FILE_143_DATA =   "shipping1,61,0"
FILE_144_DATA =   "shipping2,378,0"
FILE_145_DATA =   "shipping3,515,0"
FILE_146_DATA =   "shipping4,518,0"
FILE_147_DATA =   "shipping5,922,0"
FILE_148_DATA =   "billing1,50,0"
FILE_149_DATA =   "billing2,159,0"
FILE_150_DATA =   "billing3,519,0"
FILE_151_DATA =   "billing4,897,0"
FILE_152_DATA =   "billing5,1069,0"
FILE_153_DATA =   "search1,43,0.3"
FILE_154_DATA =   "search2,67,0.3"
FILE_155_DATA =   "search3,185,0.3"
FILE_156_DATA =   "search4,205,0.3"
FILE_157_DATA =   "search5,370,0.3"
FILE_158_DATA =   "search6,523,0.3"
FILE_159_DATA =   "search7,1731,0.3"
FILE_160_DATA =   "confirm1,43,0"
FILE_161_DATA =   "confirm2,44,0"
FILE_162_DATA =   "confirm3,80,0"
FILE_163_DATA =   "confirm4,92,0"
FILE_164_DATA =   "confirm5,97,0"
FILE_165_DATA =   "confirm6,98,0"
FILE_166_DATA =   "confirm7,130,0"
FILE_167_DATA =   "confirm8,135,0"
FILE_168_DATA =   "confirm9,135,0"
FILE_169_DATA =   "confirm10,135,0"
FILE_170_DATA =   "confirm11,223,0"
FILE_171_DATA =   "confirm12,586,0"
FILE_172_DATA =   "confirm13,1120,0"

###########################
NUM_PAGES = 13
PAGE_0_TITLE = "SPECweb2005: eCommerce - Home Page"
PAGE_1_TITLE = "SPECweb2005: eCommerce - Search Results"
PAGE_2_TITLE = "SPECweb2005: eCommerce - Browse Page"
PAGE_3_TITLE = "SPECweb2005: eCommerce - Browse Product Line"
PAGE_4_TITLE = "SPECweb2005: eCommerce - Product Details"
PAGE_5_TITLE = "SPECweb2005: eCommerce - Customize Product"
PAGE_6_TITLE = "SPECweb2005: eCommerce - Customize Product"
PAGE_7_TITLE = "SPECweb2005: eCommerce - Customize Product"
PAGE_8_TITLE = "SPECweb2005: eCommerce - Your Cart"
PAGE_9_TITLE = "SPECweb2005: eCommerce - Login Page"
PAGE_10_TITLE = "SPECweb2005: eCommerce - Shipping Details"
PAGE_11_TITLE = "SPECweb2005: eCommerce - Billing Details"
PAGE_12_TITLE = "SPECweb2005: eCommerce - Ecommerce Confirm"
PAGE_13_TITLE = "SPECweb2005: Ecommerce Error"

###########################################
#Use UNIFORM distribution
###########################################
NUM_REGION = "10"
REGION_0 = "CAD"
REGION_1 = "EUR"
REGION_2 = "JPY"
REGION_3 = "AUD"
REGION_4 = "MXN"
REGION_5 = "ARS"
REGION_6 = "BMD"
REGION_7 = "BOB"
REGION_8 = "CLP"
REGION_9 = "EGP"
REGION_10 = "HKD"
REGION_11 = "INR"
REGION_12 = "JMD"
REGION_13 = "NOK"
REGION_14 = "RUR"
REGION_15 = "SSL"
REGION_16 = "THB"
REGION_17 = "VEB"
REGION_18 = "YUM"
REGION_19 = "USD"

##########################
#FORMAT: seg=all kw=KEY_WORD
NUM_SEARCH_SEG = 5
SEARCH_SEG_0 = "all"
SEARCH_SEG_1 = "products"
SEARCH_SEG_2 = "services"
SEARCH_SEG_3 = "techsupport"
SEARCH_SEG_4 = "accessories"

###########################

# Key words to search
###########################
NUM_KEY_WORD = 19
KEY_WORD_0 = "Monitors"
KEY_WORD_1 = "DVDs"
KEY_WORD_2 = "Networks"
KEY_WORD_3 = "Softwares"
KEY_WORD_4 = "Cameras"
KEY_WORD_5 = "Cartridges"
KEY_WORD_6 = "Programs"
KEY_WORD_7 = "Disks"
KEY_WORD_8 = "Cartridges"
KEY_WORD_9 = "Projectors"
KEY_WORD_10 = "Accessories"
KEY_WORD_11 = "Processors"
KEY_WORD_12 = "Cartridges"
KEY_WORD_13 = "PDAs"
KEY_WORD_14 = "LCD-TVs"
KEY_WORD_15 = "Programs"
KEY_WORD_16 = "Boxes"
KEY_WORD_17 = "Laptops"
KEY_WORD_18 = "Accessories"

####################################
#CUSTOMER_0 = HOME USE
#CUSTOMER_1 = SMALL BUSINESS
#CUSTOMER_2 = MEDIUM/LARGE BUSINESS
#CUSTOMER_3 = GOVERNMENT/HEALTHCARE
####################################
# s=home
NUM_CUSTOMER_TYPE = 4
CUSTOMER_TYPE_SHORT_0 = "home"
CUSTOMER_TYPE_SHORT_1 = "smb"
CUSTOMER_TYPE_SHORT_2 = "mlb"
CUSTOMER_TYPE_SHORT_3 = "govt"

#CUSTOMER_TYPE_LONG_0 = "Home use"
#CUSTOMER_TYPE_LONG_1 = "Small business"
#CUSTOMER_TYPE_LONG_2 = "Medium/Large Business"
#CUSTOMER_TYPE_LONG_3 = "Government/Healthcare"

# p=MonitorsXX, XX=REGION_ID+PRODUCT_LINE_i
#NUM_PRODUCT_LINE = 20
# i=MonitorsXX, XX=REGION_ID+PRODUCT_LINE_i
NUM_PRODUCT_DETAIL = 20
PRODUCT_DETAIL_0 = "Computers"
PRODUCT_DETAIL_1 = "DIMMs"
PRODUCT_DETAIL_2 = "Processors"
PRODUCT_DETAIL_3 = "Printers"
PRODUCT_DETAIL_4 = "Cartridges"
PRODUCT_DETAIL_5 = "Disks"
PRODUCT_DETAIL_6 = "Networks"
PRODUCT_DETAIL_7 = "Cameras"
PRODUCT_DETAIL_8 = "PDAs"
PRODUCT_DETAIL_9 = "DVDs"
PRODUCT_DETAIL_10 = "Boxes"
PRODUCT_DETAIL_11 = "CD-ROMs"
PRODUCT_DETAIL_12 = "Laptops"
PRODUCT_DETAIL_13 = "Accessories"
PRODUCT_DETAIL_14 = "Software"
PRODUCT_DETAIL_15 = "Servers"
PRODUCT_DETAIL_16 = "LCD-TVs"
PRODUCT_DETAIL_17 ="Monitors"
PRODUCT_DETAIL_18 = "Programs"
PRODUCT_DETAIL_19 = "Projectors"

NUM_CUSTOMER = 10
ADDRESS_0 = "Aspen Rd"
ADDRESS_1 = "Birch Ln"
ADDRESS_2 = "CloudTree Ave"
ADDRESS_3 = "Dogwood Dr"
ADDRESS_4 = "Elm St"
ADDRESS_5 = "Heath Way"
ADDRESS_6 = "Linden Blvd"
ADDRESS_7 = "Magnolia Ave"
ADDRESS_8 = "Olive Cirle"
ADDRESS_9 = "Palm way"

CITY_0 = "Northford"
CITY_1 = "Easton"
CITY_2 = "Sudbury"
CITY_3 = "Westville"
CITY_4 = "Middletown"
CITY_5 = "Portmouth"
CITY_6 = "Bridgewater"
CITY_7 = "Littleton"
CITY_8 = "Wayland"
CITY_9 = "Upton"

US_STATE_0 = "PA"
US_STATE_1 = "OH"
US_STATE_2 = "NJ"
US_STATE_3 = "MD"
US_STATE_4 = "DE"
US_STATE_5 = "VA"
US_STATE_6 = "NC"
US_STATE_7 = "SC"
US_STATE_8 = "TN"
US_STATE_9 = "WV"

ZIP_0 = "01812"
ZIP_1 = "14112"
ZIP_2 = "13821"
ZIP_3 = "02012"
ZIP_4 = "01934"
ZIP_5 = "01732"
ZIP_6 = "01431"
ZIP_7 = "03821"
ZIP_8 = "05014"
ZIP_9 = "01967"

PHONE_0 = "123-234-3456"
PHONE_1 = "223-334-4456"
PHONE_2 = "333-445-5678"
PHONE_3 = "414-516-7271"
PHONE_4 = "555-221-0987"
PHONE_5 = "800-888-1223"
PHONE_6 = "866-912-8765"
PHONE_7 = "777-667-1256"
PHONE_8 = "929-877-4929"
PHONE_9 = "877-392-2855"

NUM_METHOD = 3
SHIP_0 = "Next_Day"
SHIP_1 = "Two_Day"
SHIP_2 = "Five_Day"

CREDITCARD_0 = "Visa"
CREDITCARD_1 = "Mastercard"
CREDITCARD_2 = "Amex"


        </xsl:text>


    </xsl:template>

</xsl:stylesheet>
