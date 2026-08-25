#!/bin/bash
echo Please enter the purpose of the sql file
read string
string2=`echo ${string// /_}`
timestamp=`date -u +'%Y%m%d%H%M'`
filename="V__${timestamp}_${string2}.sql"

# Prompt for the changeset author name
echo "Please enter your engineer name:"
read author_name

# Create the SQL file and add initial content
echo '--liquibase formatted sql' > "$filename"
echo "--changeset ${author_name}:${filename} splitStatements:true endDelimiter:;" >> "$filename"
echo ' ' >> "$filename"
echo 'select 1;' >> "$filename"