// :feature:torch-standard — standard-flavor no-op Torch capability surface.
//
// Sibling to :feature:torch, mirror of :feature:torch-rooted. Holds the no-op
// twins of the privileged Torch surfaces (TorchSysfsController +
// TorchRootCapabilities) so the standard APK can inject the same modular
// interfaces the rooted flavor does — without ever compiling against root code.
// Pulled in exclusively by the standard flavor of :app via
// `standardImplementation`.
//
// Why a module (not app/src/standard)? It makes the flavor seam symmetric with
// :feature:torch-rooted: each flavor contributes its Torch impls from a sibling
// feature module wired in via the matching `<flavor>Implementation`
// configuration, and the per-feature Hilt module travels with the impls instead
// of living in :app's flavor RootBindings. See docs/migration-guide.md.
plugins {
    id("gadget.android.feature")
}

android {
    namespace = "dev.ranzlappen.gadget.feature.torch.standard"
}

dependencies {
    // Modular Torch interfaces these no-ops implement (TorchSysfsController,
    // TorchRootCapabilities + their result/availability types). No :core:root,
    // no libsu — the standard surface is inert by construction.
    implementation(project(":feature:torch"))
}
