package me.mraxetv.beastlib.api;

import dev.dejvokep.boostedyaml.YamlDocument;
import me.mraxetv.beastlib.api.scheduler.SchedulerService;
import me.mraxetv.beastlib.api.scheduler.SchedulerServices;
import me.mraxetv.beastlib.api.client.ClientDisplayService;
import me.mraxetv.beastlib.api.client.ClientDisplayServices;
import me.mraxetv.beastlib.api.client.ClientEventService;
import me.mraxetv.beastlib.api.client.ClientEventServices;
import me.mraxetv.beastlib.api.update.UpdateService;
import me.mraxetv.beastlib.api.update.UpdateServices;
import me.mraxetv.beastlib.api.yaml.YamlFileService;
import me.mraxetv.beastlib.utils.BUtils;

public interface BeastLibAPI {

    YamlDocument getMessages();
    BUtils getUtils();

    default YamlFileService getYamlFiles() {
        return YamlFileService.getInstance();
    }

    default UpdateService getUpdateService() {
        return UpdateServices.get();
    }

    default SchedulerService getScheduler() {
        return SchedulerServices.get();
    }

    default ClientEventService getClientEvents() {
        return ClientEventServices.get();
    }

    default ClientDisplayService getClientDisplay() {
        return ClientDisplayServices.get();
    }
}
