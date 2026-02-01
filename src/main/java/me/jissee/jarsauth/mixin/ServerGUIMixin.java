package me.jissee.jarsauth.mixin;

import me.jissee.jarsauth.config.ConfigKey;
import me.jissee.jarsauth.data.DataManager;
import me.jissee.jarsauth.data.service.ConfigService;
import me.jissee.jarsauth.gui.JarsAuthGui;
import me.jissee.jarsauth.gui.Locales;
import me.jissee.jarsauth.wrap.Assert;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.gui.MinecraftServerGui;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.swing.*;

@Mixin(MinecraftServerGui.class)
public class ServerGUIMixin {

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onBuildInfoPanel(DedicatedServer p_139907_, CallbackInfo ci) {
        if(Assert.assertFalse(true)) return;
        ConfigService service = DataManager.getServerInstance().getService(ConfigService.class);
        long lang = service.getValue(ConfigKey.UI_LANGUAGE);

        Locales.setActiveLocale((int) lang);

        JButton button = new JButton();
        button.addActionListener(event -> {
            JarsAuthGui.main(new String[0]);
        });
        button.setSize(50,50);
        button.setText(Locales.getString("label.button.data"));
        ((MinecraftServerGui) (Object) this).add(button, "South");
        ((MinecraftServerGui) (Object) this).addFinalizer(JarsAuthGui::stop);

    }

}
