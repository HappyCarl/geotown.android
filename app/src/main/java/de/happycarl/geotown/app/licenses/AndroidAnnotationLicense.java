package de.happycarl.geotown.app.licenses;

import android.content.Context;

import de.psdev.licensesdialog.licenses.License;

/**
 * Created by ole on 19.10.14.
 */
public class AndroidAnnotationLicense extends License {

    String fulltext = "Licensed under the Apache License, Version 2.0 (the \"License\"); you may not\n" +
            "use this file except in compliance with the License. You may obtain a copy of\n" +
            "the License at\n" +
            "\n" +
            "http://www.apache.org/licenses/LICENSE-2.0\n" +
            "\n" +
            "Unless required by applicable law or agreed to in writing, software\n" +
            "distributed under the License is distributed on an \"AS IS\" BASIS, WITHOUT\n" +
            "WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the\n" +
            "License for the specific language governing permissions and limitations under\n" +
            "the License.\n" +
            "\n" +
            "This project uses CodeModel (http://codemodel.java.net/), which is\n" +
            "distributed under the GlassFish Dual License, which means CodeModel is\n" +
            "subject to the terms of either the GNU General Public License Version 2 only\n" +
            "(\"GPL\") or the Common Development and Distribution License(\"CDDL\").\n" +
            "\n" +
            "You may obtain a copy of the \"CDDL\" License at\n" +
            "\n" +
            "http://www.opensource.org/licenses/cddl1.php\n" +
            "\n" +
            "As per section 3.6 (\"Larger Works\") of the \"CDDL\" License, we may create a\n" +
            "Larger Work by combining Covered Software with other code not governed by\n" +
            "the terms of this License and distribute the Larger Work as a single\n" +
            "product.\n" +
            "\n" +
            "We are therefore allowed to distribute CodeModel without Modification as\n" +
            "part of AndroidAnnotations.";


    @Override
    public String getName() {
        return "Apache License";
    }

    @Override
    public String getSummaryText(Context context) {
        return "Apache License, version 2.0 with notice for CodeModels GlassFish Dual License";
    }

    @Override
    public String getFullText(Context context) {
        return fulltext;
    }

    @Override
    public String getVersion() {
        return "Version 2.0";
    }

    @Override
    public String getUrl() {
        return "https://github.com/excilys/androidannotations/blob/develop/LICENSE.txt";
    }
}
