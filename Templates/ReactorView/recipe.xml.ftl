<?xml version="1.0"?>
<recipe>
 
    <instantiate from="src/app_package/ReactorView.kt.ftl"
                   to="${escapeXmlAttribute(srcOut)}/${className}View.kt" />
    <instantiate from="src/app_package/ReactorLayout.xml.ftl"
                   to="${escapeXmlAttribute(resOut)}/layout/view_${className?lower_case}.xml" /> 
 
    <open file="${srcOut}/${className}View.kt"/>
</recipe>