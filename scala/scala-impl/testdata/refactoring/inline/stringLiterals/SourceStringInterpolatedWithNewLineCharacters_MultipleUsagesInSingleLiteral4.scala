val /*caret*/value = "line1\nline2"
s"""${value}
   |${value}
   |${value}
   |${value}
   |${value}
   |""".stripMargin
/*
s"""${"line1\nline2"}
   |${"line1\nline2"}
   |${"line1\nline2"}
   |${"line1\nline2"}
   |${"line1\nline2"}
   |""".stripMargin
 */