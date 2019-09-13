#ifndef HTML_Builder_h
#define HTML_Builder_h

#include <Arduino.h>
#ifdef ROUND_WORKAROUND
#undef round
#endif

namespace HTML_Builder
{
  const char newline='\n';
  const String breakline=R"(<br>)";
  const String html_header=R"(<html>)";
	const String html_footer=R"(</html>)";
  String create_hyperlink(String url, String title);
}

#endif
