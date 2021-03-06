package org.ovirt.engine.core.bll;

import java.util.List;

import org.ovirt.engine.core.bll.tasks.CommandCoordinatorUtil;
import org.ovirt.engine.core.bll.tasks.interfaces.CommandCallback;
import org.ovirt.engine.core.common.action.RemoveAllVmCinderDisksParameters;
import org.ovirt.engine.core.compat.CommandStatus;
import org.ovirt.engine.core.compat.Guid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RemoveAllVmCinderDisksCommandCallBack<T extends RemoveAllVmCinderDisksCommand<RemoveAllVmCinderDisksParameters>> extends CommandCallback {
    private static final Logger log = LoggerFactory.getLogger(RemoveAllVmCinderDisksCommandCallBack.class);

    @Override
    public void doPolling(Guid cmdId, List<Guid> childCmdIds) {

        boolean anyFailed = false;
        for (Guid childCmdId : childCmdIds) {
            switch (CommandCoordinatorUtil.getCommandStatus(childCmdId)) {
                case ACTIVE:
                    log.info("Waiting for child commands to complete");
                    return;
                case FAILED:
                case FAILED_RESTARTED:
                case UNKNOWN:
                    anyFailed = true;
                    break;
                default:
                    break;
            }
        }

        T command = getCommand(cmdId);
        command.getParameters().setTaskGroupSuccess(!anyFailed);
        command.setCommandStatus(anyFailed ? CommandStatus.FAILED : CommandStatus.SUCCEEDED);
        log.info("All commands have completed, status '{}'", command.getCommandStatus());
    }

    @Override
    public void onSucceeded(Guid cmdId, List<Guid> childCmdIds) {
        getCommand(cmdId).endAction();
        CommandCoordinatorUtil.removeAllCommandsInHierarchy(cmdId);
    }

    @Override
    public void onFailed(Guid cmdId, List<Guid> childCmdIds) {
        getCommand(cmdId).endAction();
        CommandCoordinatorUtil.removeAllCommandsInHierarchy(cmdId);
    }

    private T getCommand(Guid cmdId) {
        return CommandCoordinatorUtil.retrieveCommand(cmdId);
    }
}
