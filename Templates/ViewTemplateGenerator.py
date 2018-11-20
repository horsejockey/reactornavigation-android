import os
import json
import sys
from string import Template

''' Get sticker requests from Parse and upload to Dumac FTP '''
class ViewTemplateGenerator:
    def __init__(self):
        # Files
        print("init")

        fileJsonString = """
        {
            "packageName" : "com.mysmartblinds.tilt",
            "views" : {
                "login" : "Login",
                "shared" : ["Name","CheckFirmware","ChooseCalibrate","AddMore"],
                "dashboard" : ["Dashboard","LocationSetting","Settings","FirmwareUpdate","PeripheralUpdateList"],
                "rooms" : ["Rooms","RoomDetail","AddPeripheral","ControlPeripheral","ControlRollerShade","ControlSmartSwitch","ControlBridge","PeripheralStatus","PickRoom",
                    {
                        "setupbridge" : ["SetUpBridgeDiscover","SetupBridgePickWiFi"]
                    },
                    {
                        "setupsmartswitch" : ["SetUpSmartSwitchDiscover","SetupSmartSwitchScenes"]
                    },
                    {
                        "setuprollershade" : ["SetUpRollerShadeDiscover","SetupRollerShadeChooseFrontOrBackRoll","SetupRollerShadeChooseLeftOrRight","SetupRollerShadeCalibratePosition"]
                    }
                ],
                "automation" : [
                    "Automation",
                    {
                        "scenes":["Scenes","SceneDetail","PickPeripheral","RollerShadePositioning"],
                        "triggers":["Triggers","AddTrigger","PickScene","EditSetPointTrigger","EditSignificantTimeTrigger"]
                    }
                ]
            }
        }
        """

        self.fileJson = json.loads(fileJsonString)
        self.packageName = self.fileJson['packageName']
        self.viewsJson = self.fileJson['views']

        # Create default directories
        try:
            os.makedirs("generated/layout")
        except OSError:
            print("Failed to create layout directory")
        try:
            os.makedirs("generated/views")
        except OSError:
            print("Failed to create views directory")
        

    def generate(self):
            
        if isinstance(self.viewsJson, list):
            self.generateViewsList(self.viewsJson, "views")
        elif isinstance(self.viewsJson, dict):
            self.generateViewsDictionary(self.viewsJson, "views")
        elif isinstance(self.viewsJson, basestring):
            self.generateViews(self.viewsJson, "views")
                
        os._exit(0)

    def generateViewsDictionary(self, views, path):
        for directoryName, value in views.iteritems():
            if isinstance(value, list):
                self.generateViewsList(value, "%s/%s" %(path, directoryName))
            elif isinstance(value, dict):
                self.generateViewsDictionary(value, "%s/%s" %(path, directoryName))
            elif isinstance(value, basestring):
                self.generateViews(value, "%s/%s" %(path, directoryName))

    def generateViewsList(self, views, path):
        for value in views:
            if isinstance(value, list):
                self.generateViewsList(value, path)
            elif isinstance(value, dict):
                self.generateViewsDictionary(value, path)
            elif isinstance(value, basestring):
                self.generateViews(value, path)


    def generateViews(self, name, path):

        lowercaseName = name.lower()

        try:
            os.makedirs("generated/%s" % path)
        except OSError:
            print("Failed to make directory: generated/%s" % path)

        # Save layout file
        reactorViewLayout = self.generateReactorViewLayout()
        layoutFile = self.openFile("generated/layout/view_%s.xml" % lowercaseName)
        layoutFile.write(reactorViewLayout)
        layoutFile.close()

        # Save ReactorView
        reactorView = self.generateReactorView(path, name)
        viewFile = self.openFile("generated/%s/%sView.kt" % (path, name,))
        viewFile.write(reactorView)
        viewFile.close()

    def generateReactorViewLayout(self):
        return  """<?xml version="1.0" encoding="utf-8"?>
<android.support.constraint.ConstraintLayout
        xmlns:android="http://schemas.android.com/apk/res/android"
        android:layout_width="match_parent"
        android:layout_height="match_parent">

</android.support.constraint.ConstraintLayout>
"""


    def generateReactorView(self, path, name):
        template = Template(
"""package ${packageName}.${dottedPath};

import android.content.Context
import android.support.v7.widget.Toolbar
import ${packageName}.R
import io.tesseractgroup.reactornavigation.ReactorView
import io.tesseractgroup.reactornavigation.ReactorViewState
import io.tesseractgroup.reactornavigation.ViewStateConvertible


data class ${name}ViewState(val description: String = "${name}") : ReactorViewState {

    override fun view(context: Context): ReactorView {
        return ${name}View(context)
    }
}

class ${name}View(context: Context) : ReactorView(context, R.layout.view_${lowercaseName}),
    ViewStateConvertible {


    override fun state(): ReactorViewState {
        return ${name}ViewState()
    }

    override fun viewSetup(toolbar: Toolbar) {
    }

    override fun viewDidAppear() {
        super.viewDidAppear()
    }

    override fun viewDidDisappear() {
        super.viewDidDisappear()
    }

    override fun viewTearDown() {
        super.viewTearDown()
    }

}
""")

        return template.substitute(packageName=self.packageName, dottedPath=path.replace("/","."), name=name, lowercaseName=name.lower())


    def openFile(self, filename, options="w"):
        __location__ = os.path.realpath(os.path.join(os.getcwd(), os.path.dirname(__file__)))
        return open(os.path.join(__location__, filename),options)

 
if __name__ == '__main__':
    generator = ViewTemplateGenerator()
    generator.generate()
