package org.ovirt.engine.core.bll.validator;

import java.util.List;
import java.util.Set;

import org.ovirt.engine.core.bll.ImagesHandler;
import org.ovirt.engine.core.bll.IsoDomainListSyncronizer;
import org.ovirt.engine.core.bll.ValidationResult;
import org.ovirt.engine.core.bll.VmHandler;
import org.ovirt.engine.core.bll.snapshots.SnapshotsValidator;
import org.ovirt.engine.core.bll.storage.StoragePoolValidator;
import org.ovirt.engine.core.common.businessentities.BootSequence;
import org.ovirt.engine.core.common.businessentities.Disk;
import org.ovirt.engine.core.common.businessentities.DiskImage;
import org.ovirt.engine.core.common.businessentities.StoragePool;
import org.ovirt.engine.core.common.businessentities.VM;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.dal.VdcBllMessages;
import org.ovirt.engine.core.dal.dbbroker.DbFacade;
import org.ovirt.engine.core.dao.network.VmNetworkInterfaceDao;
import org.ovirt.engine.core.utils.vmproperties.VmPropertiesUtils;

public class RunVmValidator {

    public boolean validateVmProperties(VM vm, List<String> messages) {
        List<VmPropertiesUtils.ValidationError> validationErrors =
                getVmPropertiesUtils().validateVMProperties(
                        vm.getVdsGroupCompatibilityVersion(),
                        vm.getStaticData());

        if (!validationErrors.isEmpty()) {
            VmHandler.handleCustomPropertiesError(validationErrors, messages);
            return false;
        }

        return true;
    }

    public ValidationResult validateBootSequence(VM vm, BootSequence bootSequence, List<Disk> vmDisks) {
        BootSequence boot_sequence = (bootSequence != null) ?
                bootSequence : vm.getDefaultBootSequence();
        Guid storagePoolId = vm.getStoragePoolId();
        // Block from running a VM with no HDD when its first boot device is
        // HD and no other boot devices are configured
        if (boot_sequence == BootSequence.C && vmDisks.size() == 0) {
            return new ValidationResult(VdcBllMessages.VM_CANNOT_RUN_FROM_DISK_WITHOUT_DISK);
        } else {
            // If CD appears as first and there is no ISO in storage
            // pool/ISO inactive -
            // you cannot run this VM

            if (boot_sequence == BootSequence.CD
                    && getIsoDomainListSyncronizer().findActiveISODomain(storagePoolId) == null) {
                return new ValidationResult(VdcBllMessages.VM_CANNOT_RUN_FROM_CD_WITHOUT_ACTIVE_STORAGE_DOMAIN_ISO);
            } else {
                // if there is network in the boot sequence, check that the
                // vm has network,
                // otherwise the vm cannot be run in vdsm
                if (boot_sequence == BootSequence.N
                        && getVmNetworkInterfaceDao().getAllForVm(vm.getId()).size() == 0) {
                    return new ValidationResult(VdcBllMessages.VM_CANNOT_RUN_FROM_NETWORK_WITHOUT_NETWORK);
                }
            }
        }
        return ValidationResult.VALID;

    }

    /**
     * Check storage domains. Storage domain status and disk space are checked only for non-HA VMs.
     *
     * @param vm
     *            The VM to run
     * @param message
     *            The error messages to append to
     * @param isInternalExecution
     *            Command is internal?
     * @param vmImages
     *            The VM's image disks
     * @return <code>true</code> if the VM can be run, <code>false</code> if not
     */
    public boolean validateStorageDomains(VM vm,
            List<String> message,
            boolean isInternalExecution,
            List<DiskImage> vmImages) {
        if (!vm.isAutoStartup() || !isInternalExecution) {
            Set<Guid> storageDomainIds = ImagesHandler.getAllStorageIdsForImageIds(vmImages);
            MultipleStorageDomainsValidator storageDomainValidator =
                    new MultipleStorageDomainsValidator(vm.getStoragePoolId(), storageDomainIds);
            if (!validate(storageDomainValidator.allDomainsExistAndActive(), message)) {
                return false;
            }

            if (!vm.isAutoStartup()
                    && !validate(storageDomainValidator.allDomainsWithinThresholds(), message)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Check isValid only if VM is not HA VM
     */
    public boolean validateImagesForRunVm(List<String> message, List<DiskImage> vmDisks) {
        DiskImagesValidator diskImagesValidator = new DiskImagesValidator(vmDisks);
        return validate(diskImagesValidator.diskImagesNotLocked(), message);
    }

    protected VmNetworkInterfaceDao getVmNetworkInterfaceDao() {
        return DbFacade.getInstance().getVmNetworkInterfaceDao();
    }

    protected IsoDomainListSyncronizer getIsoDomainListSyncronizer() {
        return IsoDomainListSyncronizer.getInstance();
    }

    protected VmPropertiesUtils getVmPropertiesUtils() {
        return VmPropertiesUtils.getInstance();
    }

    protected boolean validate(ValidationResult validationResult, List<String> message) {
        if (!validationResult.isValid()) {
            message.add(validationResult.getMessage().name());
            if (validationResult.getVariableReplacements() != null) {
                for (String variableReplacement : validationResult.getVariableReplacements()) {
                    message.add(variableReplacement);
                }
            }
        }
        return validationResult.isValid();
    }

    // Compatibility method for static VmPoolCommandBase.canRunPoolVm
    // who uses the same validation as runVmCommand
    public boolean canRunVm(VM vm,
            List<String> messages,
            List<Disk> vmDisks,
            BootSequence bootSequence,
            StoragePool storagePool,
            boolean isInternalExecution) {
        if (!validateVmProperties(vm, messages)) {
            return false;
        }
        ValidationResult result = validateBootSequence(vm, bootSequence, vmDisks);
        if (!result.isValid()) {
            messages.add(result.getMessage().toString());
            return false;
        }
        result = new VmValidator(vm).vmNotLocked();
        if (!result.isValid()) {
            messages.add(result.getMessage().toString());
            return false;
        }
        result = new SnapshotsValidator().vmNotDuringSnapshot(vm.getId());
        if (!result.isValid()) {
            messages.add(result.getMessage().toString());
            return false;
        }
        List<DiskImage> images = ImagesHandler.filterImageDisks(vmDisks, true, false);
        if (!images.isEmpty()) {
            result = new StoragePoolValidator(storagePool).isUp();
            if (!result.isValid()) {
                messages.add(result.getMessage().toString());
                return false;
            }
            if (!validateStorageDomains(vm, messages, isInternalExecution, images)) {
                return false;
            }
            if (!validateImagesForRunVm(messages, images)) {
                return false;
            }
        }
        return true;
    }

}
