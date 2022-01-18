#!/bin/sh

# -- Copyright (c) 2018-2022  silvio.brandani <support@tcapture.net>. All rights reserved.

runJREApplication()
{
        verifyJRE "$JAVA_EXECUTABLE_PATH" $JAVA_MINIMUM_VERSION $JAVA_BITNESS_REQUIRED

        if [ $? -eq 0 ];
        then
                "$JAVA_EXECUTABLE_PATH" "$@"
        else
                echo $ErrorMessage
        fi
}

verifyJRE()
{
        JAVA_PATH="$1"
        JAVA_MIN_VERSION="$2"
        JAVA_BITNESS="$3"

        IS_VALID_JRE=0

	if [ "`uname`" = "SunOS" ]; then
	        AWK=/usr/xpg4/bin/awk # awk utility
	else
        	AWK=awk
	fi

        if [ -f "$JAVA_PATH" ];
        then
                JRE_BITNESS_FOUND=32            # Set default to 32-bit
                JAVA_VERSION_OUTPUT=`"$JAVA_PATH" -version 2>&1`
                JRE_VERSION_FOUND=`echo $JAVA_VERSION_OUTPUT | $AWK -F '"' '/version/ {print $2}' | cut -f2 -d"\"" | cut -f1,2 -d"."`

                st=`expr $JAVA_MIN_VERSION '<=' $JRE_VERSION_FOUND`
                if [ $st -eq 1 ];
                then
                        if echo $JAVA_VERSION_OUTPUT | grep "64-Bit" &> /dev/null
                        then
                                JRE_BITNESS_FOUND=64
                        elif echo $JAVA_VERSION_OUTPUT | grep "ppc64-64" &> /dev/null
                        then
                                JRE_BITNESS_FOUND=64
                        elif echo $JAVA_VERSION_OUTPUT | grep "ppc64le-64" &> /dev/null
                        then
                                JRE_BITNESS_FOUND=64
                        fi

                        if [ $JAVA_BITNESS = $JRE_BITNESS_FOUND ];
                        then
                                IS_VALID_JRE=1
			elif [ $JAVA_BITNESS = 0 ];
                        then
                                IS_VALID_JRE=1
                        else
                                # Error for incorrect Java bitness
                                ErrorMessage="$JAVA_BITNESS bit java not found."
                                return 1
                        fi
                else
                        # Error for Minimum Java version not found
                        ErrorMessage="Java $JAVA_MIN_VERSION or greater is not found on your machine."
                        return 2
                fi
        fi


        if [ $IS_VALID_JRE = "0" ];
        then
                ErrorMessage="Unable to find JRE in path."
                return 3
        fi

        return 0
}

