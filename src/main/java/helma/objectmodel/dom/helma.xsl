<?xml version="1.0" encoding="iso-8859-15"?>

<xsl:stylesheet
   version="1.0"
   xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
   xmlns:hop="http://www.helma.org/docs/guide/features/database"
>

<xsl:output method="html"/>

<xsl:variable name="id" select="/xmlroot/hopobject/@id"/>
<xsl:variable name="name" select="/xmlroot/hopobject/@name"/>
<xsl:variable name="prototype" select="/xmlroot/hopobject/@prototype"/>
<xsl:variable name="parent" select="/xmlroot/hopobject/hop:parent"/>
<xsl:variable name="children" select="/xmlroot/hopobject/hop:child"/>

<xsl:template match="/">
   <html>
   <head>
   <title><xsl:value-of select="$id"/>.xml (Hop XML Database File)</title>
   <meta http-equiv="Content-Type" content="text/html; charset=iso-8859-15" />
   </head>  
   <body bgcolor="white">

   <!-- main navigation -->
   <h2>
   <xsl:if test="$id = 0">root</xsl:if>
   <xsl:if test="$id &gt; 0">
      <a href="0.xml">root</a> : HopObject <xsl:value-of select="$id"/>
   </xsl:if>
   </h2>

   <!-- table header -->
   <table border="0" cellspacing="1" cellpadding="5" bgcolor="gray">
   <tr bgcolor="white">
   <th>Name</th>
   <th>Value</th>
   </tr>

   <!-- _name, _prototype and _parent properties -->
   <xsl:if test="$name">
      <xsl:call-template name="getOutputItem">
         <xsl:with-param name="name">_name</xsl:with-param>
         <xsl:with-param name="value" select="$name"/>
      </xsl:call-template>
   </xsl:if>
   <xsl:if test="$prototype">
      <xsl:call-template name="getOutputItem">
         <xsl:with-param name="name">_prototype</xsl:with-param>
         <xsl:with-param name="value" select="$prototype"/>
      </xsl:call-template>
   </xsl:if>
   <xsl:if test="$parent">
      <xsl:call-template name="getOutputItem">
         <xsl:with-param name="name">_parent</xsl:with-param>
         <xsl:with-param name="value">
            HopObject <xsl:value-of select="$parent/@idref"/>
         </xsl:with-param>
         <xsl:with-param name="href">
            <xsl:value-of select="$parent/@idref"/>.xml
         </xsl:with-param>
      </xsl:call-template>
   </xsl:if>

   <!-- _children collection -->
   <xsl:if test="count($children) &gt; 0">
      <tr bgcolor="white">
      <td valign="top" nowrap="nowrap">_children</td>
      <td>
      <xsl:for-each select="$children">
         <xsl:sort select="@idref" data-type="number"/>
         <a href="{@idref}.xml"><nowrap><xsl:value-of select="@prototyperef"/>
         <xsl:text> </xsl:text><xsl:value-of select="@idref"/></nowrap></a>
         <xsl:if test="position() &lt; count($children)">
            <xsl:text>, </xsl:text>
         </xsl:if> 
      </xsl:for-each>
      </td>
      </tr>
   </xsl:if>

   <!-- primitive properties -->
   <xsl:for-each select="/xmlroot/hopobject/*">
      <xsl:sort select="concat(@propertyname, name())"/>
      <xsl:choose>
         <xsl:when test="name() = 'hop:parent'"/>
         <xsl:when test="name() = 'hop:child'"/>
         <xsl:when test="@idref">
            <xsl:call-template name="getOutputItem">
               <xsl:with-param name="name">
                  <xsl:call-template name="getPropertyName"/>
               </xsl:with-param>
               <xsl:with-param name="value">
                  HopObject <xsl:value-of select="@idref"/>
               </xsl:with-param>
               <xsl:with-param name="href">
                  <xsl:value-of select="@idref"/>.xml
               </xsl:with-param>
            </xsl:call-template>
         </xsl:when>
         <xsl:otherwise>
            <xsl:call-template name="getOutputItem">
               <xsl:with-param name="name">
                  <xsl:call-template name="getPropertyName"/>
               </xsl:with-param>
               <xsl:with-param name="value"><xsl:value-of select="text()"/></xsl:with-param>
            </xsl:call-template>
         </xsl:otherwise>
      </xsl:choose>
   </xsl:for-each>

   </table>
   </body>
   </html>
</xsl:template>

<!-- helper template to compose a hopobject's name -->
<xsl:template name="getName">
   <xsl:param name="name"/>
   <xsl:choose>
      <xsl:when test="substring-after($name, 'HopObject ') = '0'">
         root
      </xsl:when>
      <xsl:otherwise>
         <xsl:value-of select="$name"/>
      </xsl:otherwise>
   </xsl:choose>
</xsl:template>

<!-- helper template to compose a property's name: if the element's 
     name is "property", the property name is in an attribute called
     "propertyname". Otherwise, the element name is the element's name -->
<xsl:template name="getPropertyName">
   <xsl:choose>
      <xsl:when test="name() = 'property'">
         <xsl:value-of select="@propertyname"/>
      </xsl:when>
      <xsl:otherwise>
         <xsl:value-of select="name()"/>
      </xsl:otherwise>
   </xsl:choose>
</xsl:template>

<!-- helper template to compose a table row containing a property's data -->
<xsl:template name="getOutputItem">
   <xsl:param name="name"/>
   <xsl:param name="value"/>
   <xsl:param name="href"/>

   <xsl:variable name="display">
      <xsl:call-template name="getName">
         <xsl:with-param name="name" select="$value"/>
      </xsl:call-template>
   </xsl:variable>

   <tr bgcolor="white">
   <td valign="top" nowrap="nowrap"><xsl:value-of select="$name"/></td>
   <td><xsl:choose>
      <xsl:when test="$href">
         <a href="{$href}"><xsl:value-of select="$display"/></a>
      </xsl:when>
      <xsl:otherwise>
         <xsl:value-of select="$display"/>
      </xsl:otherwise>
   </xsl:choose></td>
   </tr>
</xsl:template>

</xsl:stylesheet>
