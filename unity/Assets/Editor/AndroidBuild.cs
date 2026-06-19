#if UNITY_EDITOR
using System.IO;
using UnityEditor;
using UnityEditor.SceneManagement;
using UnityEngine;

namespace Wildland3D.Editor
{
    public static class AndroidBuild
    {
        public static void PerformBuild()
        {
            Directory.CreateDirectory("Assets/Scenes");
            Directory.CreateDirectory("Builds/Android");

            // Create a deterministic empty scene. GameBootstrap creates the full game at runtime.
            var scene = EditorSceneManager.NewScene(NewSceneSetup.EmptyScene, NewSceneMode.Single);
            var bootstrap = new GameObject("Wildland3D Bootstrap");
            bootstrap.AddComponent<GameBootstrap>();
            EditorSceneManager.SaveScene(scene, "Assets/Scenes/Main.unity");

            PlayerSettings.companyName = "Arena";
            PlayerSettings.productName = "Wildland 3D";
            PlayerSettings.SetApplicationIdentifier(BuildTargetGroup.Android, "com.arena.wildland3d.unity");
            PlayerSettings.bundleVersion = "0.3.0";
            PlayerSettings.Android.bundleVersionCode = 1;
            PlayerSettings.defaultInterfaceOrientation = UIOrientation.LandscapeLeft;
            PlayerSettings.allowedAutorotateToLandscapeLeft = true;
            PlayerSettings.allowedAutorotateToLandscapeRight = true;
            PlayerSettings.allowedAutorotateToPortrait = false;
            PlayerSettings.allowedAutorotateToPortraitUpsideDown = false;
            PlayerSettings.Android.minSdkVersion = AndroidSdkVersions.AndroidApiLevel23;
            PlayerSettings.Android.targetSdkVersion = AndroidSdkVersions.AndroidApiLevelAuto;

            EditorUserBuildSettings.SwitchActiveBuildTarget(BuildTargetGroup.Android, BuildTarget.Android);
            EditorUserBuildSettings.androidBuildSystem = AndroidBuildSystem.Gradle;
            EditorUserBuildSettings.buildAppBundle = false;

            var options = new BuildPlayerOptions
            {
                scenes = new[] { "Assets/Scenes/Main.unity" },
                locationPathName = "Builds/Android/wildland-3d-unity.apk",
                target = BuildTarget.Android,
                options = BuildOptions.None
            };

            var report = BuildPipeline.BuildPlayer(options);
            if (report.summary.result != UnityEditor.Build.Reporting.BuildResult.Succeeded)
                throw new System.Exception("Unity Android build failed: " + report.summary.result);
        }
    }
}
#endif
