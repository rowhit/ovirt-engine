package org.ovirt.engine.core.bll;

import java.util.List;

import org.ovirt.engine.core.bll.job.ExecutionHandler;
import org.ovirt.engine.core.common.action.AddVmFromTemplateParameters;
import org.ovirt.engine.core.common.action.CreateCloneOfTemplateParameters;
import org.ovirt.engine.core.common.action.VdcActionType;
import org.ovirt.engine.core.common.action.VdcReturnValueBase;
import org.ovirt.engine.core.common.businessentities.DiskImage;
import org.ovirt.engine.core.common.businessentities.DiskImageBase;
import org.ovirt.engine.core.common.businessentities.VMStatus;
import org.ovirt.engine.core.common.errors.VdcBLLException;
import org.ovirt.engine.core.common.errors.VdcBllErrors;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.dal.dbbroker.DbFacade;

@LockIdNameAttribute
public class AddVmFromTemplateCommand<T extends AddVmFromTemplateParameters> extends AddVmCommand<T> {

    public AddVmFromTemplateCommand(T parameters) {
        super(parameters);
        parameters.setDontCheckTemplateImages(true);
    }

    protected AddVmFromTemplateCommand(Guid commandId) {
        super(commandId);
    }

    @Override
    protected boolean validateIsImagesOnDomains() {
        return true;
    }

    @Override
    protected void ExecuteVmCommand() {
        super.ExecuteVmCommand();
        // override template id to blank
        getParameters().OriginalTemplate = getVm().getvmt_guid();
        VmTemplateHandler.lockVmTemplateInTransaction(getParameters().OriginalTemplate, getCompensationContext());
        getVm().setvmt_guid(VmTemplateHandler.BlankVmTemplateId);
        getVm().getStaticData().setQuotaId(getParameters().getVmStaticData().getQuotaId());
        DbFacade.getInstance().getVmStaticDAO().update(getVm().getStaticData());
    }

    @Override
    protected boolean AddVmImages() {
        if (getVmTemplate().getDiskMap().size() > 0) {
            if (getVm().getstatus() != VMStatus.Down) {
                log.error("Cannot add images. VM is not Down");
                throw new VdcBLLException(VdcBllErrors.IRS_IMAGE_STATUS_ILLEGAL);
            }
            VmHandler.LockVm(getVm().getDynamicData(), getCompensationContext());
            for (DiskImage disk : getVmTemplate().getDiskMap().values()) {
                DiskImageBase diskInfo = getParameters().getDiskInfoDestinationMap().get(disk.getId());
                CreateCloneOfTemplateParameters p = new CreateCloneOfTemplateParameters(disk.getImageId(),
                        getParameters().getVmStaticData().getId(), diskInfo);
                p.setStorageDomainId(disk.getstorage_ids().get(0));
                p.setDestStorageDomainId(diskInfoDestinationMap.get(disk.getId()).getstorage_ids().get(0));
                p.setVmSnapshotId(getVmSnapshotId());
                p.setParentCommand(VdcActionType.AddVmFromTemplate);
                p.setEntityId(getParameters().getEntityId());
                p.setQuotaId(diskInfoDestinationMap.get(disk.getId()).getQuotaId() != null ? diskInfoDestinationMap.get(disk.getId())
                        .getQuotaId()
                        : null);
                VdcReturnValueBase result = Backend.getInstance().runInternalAction(
                                VdcActionType.CreateCloneOfTemplate,
                                p,
                                ExecutionHandler.createDefaultContexForTasks(getExecutionContext()));
                getParameters().getImagesParameters().add(p);

                /**
                 * if couldnt create snapshot then stop the transaction and the command
                 */
                if (!result.getSucceeded()) {
                    throw new VdcBLLException(VdcBllErrors.VolumeCreationError);
                } else {
                    getTaskIdList().addAll(result.getInternalTaskIdList());
                    newDiskImages.add((DiskImage) result.getActionReturnValue());
                }
            }
        }
        return true;
    }

    @Override
    protected boolean canDoAction() {
        boolean retValue = super.canDoAction();
        if (retValue) {
            for (DiskImage dit : getVmTemplate().getDiskMap().values()) {
                retValue =
                        ImagesHandler.CheckImageConfiguration(destStorages.get(diskInfoDestinationMap.get(dit.getId()).getstorage_ids().get(0))
                                .getStorageStaticData(),
                                diskInfoDestinationMap.get(dit.getId()),
                                getReturnValue().getCanDoActionMessages());
                if (!retValue) {
                    break;
                }
            }
        }
        return retValue;
    }

    @Override
    protected int getNeededDiskSize(Guid storageId) {
        double actualSize = 0;
        List<DiskImage> disks = storageToDisksMap.get(storageId);
        for (DiskImage disk : disks) {
            actualSize += disk.getActualSize();
        }
        return (int) actualSize;
    }

    @Override
    protected void EndSuccessfully() {
        super.EndSuccessfully();
        VmTemplateHandler.UnLockVmTemplate(getParameters().OriginalTemplate);
    }

    @Override
    protected void EndWithFailure() {
        super.EndWithFailure();
        VmTemplateHandler.UnLockVmTemplate(getParameters().OriginalTemplate);
    }
}
