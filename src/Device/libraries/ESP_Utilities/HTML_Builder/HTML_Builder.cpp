#include "HTML_Builder.h"

namespace HTML_Builder
{
    String create_hyperlink(String url, String title)
    {
      String retval =
      R"(
        <a href="
  		)"
  		+(String)url+
      R"(
  		    ">
      )"
      +title+
      R"(
        </a>
      )"
      ;

      return retval;
    }
}
