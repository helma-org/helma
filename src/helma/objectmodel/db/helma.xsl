<?xml version="1.0" encoding="iso-8859-15"?>

<xsl:stylesheet
   version="1.0"
   xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
   xmlns:hop="http://www.helma.org/docs/guide/features/database"
>

<xsl:output method="html"/>

<xsl:template match="/">
   <xsl:variable name="id" select="/xmlroot/hopobject/@id"/>

   <html>
   <head>
   <title><xsl:value-of select="$id"/>.xml (Hop XML Database File)</title>
   <meta http-equiv="Content-Type" content="text/html; charset=iso-8859-15" />
   </head>  
   <body bgcolor="white">

   <h2>
   <xsl:if test="$id = 0">root</xsl:if>
   <xsl:if test="$id &gt; 0">
      <a href="0.xml">root</a> : HopObject <xsl:value-of select="$id"/>
   </xsl:if>
   </h2>

   <table border="0" cellspacing="1" cellpadding="5" bgcolor="gray">
   <tr bgcolor="white">
   <th>Name</th>
   <th>Value</th>
   </tr>

   <xsl:variable name="name" select="/xmlroot/hopobject/@name"/>
   <xsl:if test="$name">
      <xsl:call-template name="property">
         <xsl:with-param name="name">_name</xsl:with-param>
         <xsl:with-param name="value" select="$name"/>
      </xsl:call-template>
   </xsl:if>

   <xsl:variable name="prototype" select="/xmlroot/hopobject/@prototype"/>
   <xsl:if test="$prototype">
      <xsl:call-template name="property">
         <xsl:with-param name="name">_prototype</xsl:with-param>
         <xsl:with-param name="value" select="$prototype"/>
      </xsl:call-template>
   </xsl:if>

   <xsl:variable name="parent" select="/xmlroot/hopobject/hop:parent"/>
   <xsl:if test="$parent">
      <xsl:call-template name="property">
         <xsl:with-param name="name">_parent</xsl:with-param>
         <xsl:with-param name="value">
            HopObject <xsl:value-of select="$parent/@idref"/>
         </xsl:with-param>
         <xsl:with-param name="href">
            <xsl:value-of select="$parent/@idref"/>.xml
         </xsl:with-param>
      </xsl:call-template>
   </xsl:if>

   <xsl:variable name="children" select="/xmlroot/hopobject/hop:child"/>
   <xsl:if test="count($children) &gt; 0">
      <tr bgcolor="white">
      <td valign="top" nowrap="nowrap">_children</td>
      <td>
      <xsl:for-each select="/xmlroot/hopobject/hop:child">
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

   <xsl:for-each select="/xmlroot/hopobject/*">
      <!-- FIXME: sorting doesn't work completely. What we ought to do  
           is sort by (@propertyname OR name()). Don't know how to 
           express that in XPath. --> 
      <xsl:sort select="@propertyname"/>
      <xsl:sort select="name()"/>
      <xsl:choose>
         <xsl:when test="name() = 'hop:parent'"/>
         <xsl:when test="name() = 'hop:child'"/>
         <xsl:when test="@idref">
            <xsl:call-template name="property">
               <xsl:with-param name="name">
                  <!-- if element name is "property", property name is in an attribute called
                       "propertyname". Otherwise, the element name is the property name -->
                  <xsl:if test="name() = 'property'">
                     <xsl:value-of select="@propertyname"/>
                  </xsl:if>
                  <xsl:if test="name() != 'property'">
                     <xsl:value-of select="name()"/>
                  </xsl:if>
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
            <xsl:call-template name="property">
               <xsl:with-param name="name">
                  <!-- if element name is "property", property name is in an attribute called
                       "propertyname". Otherwise, the element name is the property name -->
                  <xsl:if test="name() = 'property'">
                     <xsl:value-of select="@propertyname"/>
                  </xsl:if>
                  <xsl:if test="name() != 'property'">
                     <xsl:value-of select="name()"/>
                  </xsl:if>
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

<xsl:template name="property">
   <xsl:param name="name"/>
   <xsl:param name="type"/>
   <xsl:param name="href"/>
   <xsl:param name="value"/>
   <xsl:param name="display">
      <xsl:call-template name="getName">
         <xsl:with-param name="name" select="$value"/>
      </xsl:call-template>
   </xsl:param>

   <tr bgcolor="white">
   <td valign="top" nowrap="nowrap"><xsl:value-of select="$name"/></td>
   <td><xsl:if test="$href">
      <a href="{$href}"><xsl:value-of select="$display"/></a>
   </xsl:if>
   <xsl:if test="$href = ''">
      <xsl:value-of select="$display"/>
   </xsl:if>

</td>
   </tr>
</xsl:template>

<xsl:template name="getName">
   <xsl:param name="name"/>
   <xsl:param name="isRoot" select="substring-after($name, 'HopObject ') = '0'"/>
   <xsl:if test="$isRoot">
      root
   </xsl:if>
   <xsl:if test="$isRoot = false">
      <xsl:value-of select="$name"/>
   </xsl:if>
</xsl:template>

</xsl:stylesheet>
