package org.ovirt.engine.ui.uicommonweb.models.vms;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.ovirt.engine.core.common.businessentities.ActionGroup;
import org.ovirt.engine.core.common.businessentities.DisplayType;
import org.ovirt.engine.core.common.businessentities.StorageDomain;
import org.ovirt.engine.core.common.businessentities.StorageDomainStatus;
import org.ovirt.engine.core.common.businessentities.StoragePool;
import org.ovirt.engine.core.common.businessentities.VDSGroup;
import org.ovirt.engine.core.common.businessentities.VM;
import org.ovirt.engine.core.common.businessentities.VmBase;
import org.ovirt.engine.core.common.businessentities.VmTemplate;
import org.ovirt.engine.core.common.businessentities.comparators.NameableComparator;
import org.ovirt.engine.core.common.businessentities.storage.Disk;
import org.ovirt.engine.core.common.businessentities.storage.DiskStorageType;
import org.ovirt.engine.core.common.businessentities.storage.DiskImage;
import org.ovirt.engine.core.common.businessentities.storage.VolumeType;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.ui.frontend.AsyncQuery;
import org.ovirt.engine.ui.frontend.INewAsyncCallback;
import org.ovirt.engine.ui.uicommonweb.Linq;
import org.ovirt.engine.ui.uicommonweb.builders.BuilderExecutor;
import org.ovirt.engine.ui.uicommonweb.builders.vm.CommonVmBaseToUnitBuilder;
import org.ovirt.engine.ui.uicommonweb.dataprovider.AsyncDataProvider;
import org.ovirt.engine.ui.uicommonweb.models.EntityModel;
import org.ovirt.engine.ui.uicommonweb.models.ListModel;
import org.ovirt.engine.ui.uicommonweb.models.SystemTreeItemModel;
import org.ovirt.engine.ui.uicommonweb.models.SystemTreeItemType;
import org.ovirt.engine.ui.uicommonweb.models.storage.DisksAllocationModel;
import org.ovirt.engine.ui.uicompat.ConstantsManager;

public class NewTemplateVmModelBehavior extends VmModelBehaviorBase<UnitVmModel>
{
    private final VM vm;

    public NewTemplateVmModelBehavior(VM vm)
    {
        this.vm = vm;
    }

    protected VM getVm() {
        return vm;
    }

    @Override
    public void initialize(SystemTreeItemModel systemTreeSelectedItem)
    {
        super.initialize(systemTreeSelectedItem);
        getModel().getVmInitEnabled().setEntity(vm.getVmInit() != null);
        getModel().getVmInitModel().init(vm.getStaticData());

        getModel().getVmType().setIsChangable(true);
        getModel().getCopyPermissions().setIsAvailable(true);

        DisksAllocationModel disksAllocationModel = getModel().getDisksAllocationModel();
        disksAllocationModel.setIsAliasChangable(true);

        AsyncDataProvider.getInstance().getDataCenterById(new AsyncQuery(getModel(),
                new INewAsyncCallback() {
                    @Override
                    public void onSuccess(Object target, Object returnValue) {

                        final StoragePool dataCenter = (StoragePool) returnValue;
                        if (dataCenter == null)
                        {
                            disableNewTemplateModel(ConstantsManager.getInstance()
                                    .getConstants()
                                    .dataCenterIsNotAccessibleMsg());
                        }
                        else
                        {

                            AsyncDataProvider.getInstance().getClusterListByService(
                                    new AsyncQuery(getModel(), new INewAsyncCallback() {

                                        @Override
                                        public void onSuccess(Object target, Object returnValue) {
                                            UnitVmModel model = (UnitVmModel) target;

                                            List<VDSGroup> clusters = (List<VDSGroup>) returnValue;

                                            List<VDSGroup> filteredClusters =
                                                    AsyncDataProvider.getInstance().filterByArchitecture(clusters,
                                                            vm.getClusterArch());

                                            model.setDataCentersAndClusters(model,
                                                    Arrays.asList(dataCenter),
                                                    filteredClusters,
                                                    vm.getVdsGroupId());

                                            initTemplate();

                                        }
                                    }),
                                    true,
                                    false);

                            AsyncDataProvider.getInstance().isSoundcardEnabled(new AsyncQuery(getModel(),
                                    new INewAsyncCallback() {

                                        @Override
                                        public void onSuccess(Object model, Object returnValue) {
                                            getModel().getIsSoundcardEnabled().setEntity((Boolean) returnValue);
                                        }
                                    }), vm.getId());
                        }

                    }
                }),
                vm.getStoragePoolId());
    }

    protected void updateTemplate()
    {
        final DataCenterWithCluster dataCenterWithCluster =
                (DataCenterWithCluster) getModel().getDataCenterWithClustersList().getSelectedItem();
        StoragePool dataCenter = dataCenterWithCluster == null ? null : dataCenterWithCluster.getDataCenter();
        if (dataCenter == null) {
            return;
        }

        // Filter according to system tree selection.
        if (getSystemTreeSelectedItem() != null && getSystemTreeSelectedItem().getType() == SystemTreeItemType.Storage)
        {
            final StorageDomain storage = (StorageDomain) getSystemTreeSelectedItem().getEntity();

            AsyncDataProvider.getInstance().getTemplateListByDataCenter(new AsyncQuery(getModel(),
                    new INewAsyncCallback() {
                        @Override
                        public void onSuccess(Object target1, Object returnValue1) {

                            NewTemplateVmModelBehavior behavior1 = NewTemplateVmModelBehavior.this;
                            AsyncDataProvider.getInstance().getTemplateListByStorage(new AsyncQuery(new Object[] { behavior1,
                                    returnValue1 },
                                    new INewAsyncCallback() {
                                        @Override
                                        public void onSuccess(Object target2, Object returnValue2) {

                                            Object[] array2 = (Object[]) target2;
                                            NewTemplateVmModelBehavior behavior2 = (NewTemplateVmModelBehavior) array2[0];
                                            ArrayList<VmTemplate> templatesByDataCenter =
                                                    (ArrayList<VmTemplate>) array2[1];
                                            ArrayList<VmTemplate> templatesByStorage =
                                                    (ArrayList<VmTemplate>) returnValue2;
                                            VmTemplate blankTemplate =
                                                    Linq.firstOrDefault(templatesByDataCenter,
                                                            new Linq.TemplatePredicate(Guid.Empty));
                                            if (blankTemplate != null)
                                            {
                                                templatesByStorage.add(0, blankTemplate);
                                            }

                                            ArrayList<VmTemplate> templateList = AsyncDataProvider.getInstance().filterTemplatesByArchitecture(templatesByStorage,
                                                            dataCenterWithCluster.getCluster().getArchitecture());

                                            behavior2.postInitTemplate(templateList);

                                        }
                                    }),
                                    storage.getId());

                        }
                    }),
                    dataCenter.getId());
        }
        else
        {
            AsyncDataProvider.getInstance().getTemplateListByDataCenter(new AsyncQuery(getModel(),
                    new INewAsyncCallback() {
                        @Override
                        public void onSuccess(Object target, Object returnValue) {

                            NewTemplateVmModelBehavior behavior = NewTemplateVmModelBehavior.this;

                            ArrayList<VmTemplate> templates = (ArrayList<VmTemplate>) returnValue;

                            behavior.postInitTemplate(AsyncDataProvider.getInstance().filterTemplatesByArchitecture(templates,
                                    dataCenterWithCluster.getCluster().getArchitecture()));

                        }
                    }), dataCenter.getId());
        }
    }

    private void postInitTemplate(List<VmTemplate> templates) {
        List<VmTemplate> baseTemplates = keepBaseTemplates(templates);

        VmTemplate currentTemplate = Linq.firstOrDefault(templates,
                new Linq.TemplatePredicate(vm.getVmtGuid()));

        getModel().getBaseTemplate().setItems(baseTemplates);

        getModel().getBaseTemplate().setSelectedItem(Linq.firstOrDefault(baseTemplates,
                new Linq.TemplatePredicate(currentTemplate.getBaseTemplateId())));
    }

    @Override
    public void dataCenterWithClusterSelectedItemChanged()
    {
        super.dataCenterWithClusterSelectedItemChanged();

        // If a VM has at least one disk, present its storage domain.
        AsyncDataProvider.getInstance().getVmDiskList(new AsyncQuery(getModel(),
                new INewAsyncCallback() {
                    @Override
                    public void onSuccess(Object target, Object returnValue) {

                        NewTemplateVmModelBehavior behavior = NewTemplateVmModelBehavior.this;
                        ArrayList<Disk> imageDisks = new ArrayList<Disk>();
                        ArrayList<Disk> vmDisks = (ArrayList<Disk>) returnValue;

                        for (Disk disk : vmDisks) {
                            if (disk.getDiskStorageType() == DiskStorageType.IMAGE && !disk.isShareable()
                                    && !disk.isDiskSnapshot()) {
                                imageDisks.add(disk);
                            }
                        }

                        behavior.initStorageDomains();
                        initDisks(imageDisks);

                        VmModelHelper.sendWarningForNonExportableDisks(getModel(),
                                vmDisks,
                                VmModelHelper.WarningType.VM_TEMPLATE);
                    }
                }),
                vm.getId(),
                true);
    }

    private void initDisks(ArrayList<Disk> disks)
    {
        Collections.sort(disks, new Linq.DiskByAliasComparer());
        ArrayList<DiskModel> list = new ArrayList<DiskModel>();

        for (Disk disk : disks)
        {
            DiskModel diskModel = new DiskModel();

            if (disk.getDiskStorageType() == DiskStorageType.IMAGE) {
                DiskImage diskImage = (DiskImage) disk;
                EntityModel<Integer> size = new EntityModel<Integer>();
                size.setEntity((int) diskImage.getSizeInGigabytes());
                diskModel.setSize(size);
                ListModel volumes = new ListModel();
                volumes.setItems((diskImage.getVolumeType() == VolumeType.Preallocated ? new ArrayList<VolumeType>(Arrays.asList(new VolumeType[] {VolumeType.Preallocated}))
                        : AsyncDataProvider.getInstance().getVolumeTypeList()));
                volumes.setSelectedItem(diskImage.getVolumeType());
                diskModel.setVolumeType(volumes);
                diskModel.getAlias().setEntity(diskImage.getDiskAlias());
            }

            diskModel.setDisk(disk);
            list.add(diskModel);
        }
        getModel().setDisks(list);
    }

    @Override
    public void postDataCenterWithClusterSelectedItemChanged()
    {
        updateQuotaByCluster(null, null);
        updateMemoryBalloon();
        updateCpuSharesAvailability();
        updateVirtioScsiAvailability();
        updateOSValues();
        updateTemplate();
        updateNumOfSockets();
        if(getModel().getSelectedCluster() != null) {
            updateCpuProfile(getModel().getSelectedCluster().getId(), getModel().getSelectedCluster()
                    .getCompatibilityVersion(), vm.getCpuProfileId());
        }
        updateCustomPropertySheet();
        getModel().getCustomPropertySheet().deserialize(vm.getCustomProperties());
    }

    @Override
    public void defaultHost_SelectedItemChanged()
    {
    }

    @Override
    public void provisioning_SelectedItemChanged()
    {
    }

    @Override
    public void oSType_SelectedItemChanged()
    {
    }

    @Override
    public void updateMinAllocatedMemory()
    {
    }

    private void initTemplate() {
        // Update model state according to VM properties.
        buildModel(this.vm.getStaticData(), new BuilderExecutor.BuilderExecutionFinished<VmBase, UnitVmModel>() {
            @Override
            public void finished(VmBase source, UnitVmModel destination) {
                updateSelectedCdImage(vm.getStaticData());
                updateTimeZone(vm.getTimeZone());
                updateConsoleDevice(vm.getId());

                getModel().getStorageDomain().setIsChangable(true);
                getModel().getProvisioning().setIsAvailable(false);

                // Select display protocol.
                DisplayType displayType = vm.getDefaultDisplayType();
                if (getModel().getDisplayType().getItems().contains(displayType)) {
                    getModel().getDisplayType().setSelectedItem(displayType);
                }

                initPriority(vm.getPriority());
            }
        });
    }

    @Override
    protected void buildModel(VmBase vmBase,
                              BuilderExecutor.BuilderExecutionFinished<VmBase, UnitVmModel> callback) {
        new BuilderExecutor<>(callback,
                              new CommonVmBaseToUnitBuilder())
                .build(vmBase, getModel());
    }

    @Override
    public void initStorageDomains()
    {
        AsyncDataProvider.getInstance().getPermittedStorageDomainsByStoragePoolId(new AsyncQuery(getModel(),
                new INewAsyncCallback() {
                    @Override
                    public void onSuccess(Object target, Object returnValue) {
                        NewTemplateVmModelBehavior behavior = NewTemplateVmModelBehavior.this;
                        ArrayList<StorageDomain> activeStorageDomainList =
                                new ArrayList<StorageDomain>();

                        for (StorageDomain storageDomain : (ArrayList<StorageDomain>) returnValue)
                        {
                            if (storageDomain.getStatus() == StorageDomainStatus.Active
                                    && storageDomain.getStorageDomainType().isDataDomain())
                            {
                                activeStorageDomainList.add(storageDomain);
                            }
                        }

                        for (DiskModel diskModel : getModel().getDisks()) {
                            if (diskModel.getDisk().getDiskStorageType() == DiskStorageType.IMAGE) {
                                DiskImage diskImage = (DiskImage) diskModel.getDisk();
                                ArrayList<StorageDomain> activeDiskStorages =
                                        Linq.getStorageDomainsByIds(diskImage.getStorageIds(), activeStorageDomainList);

                                if (activeDiskStorages.isEmpty()) {
                                    behavior.disableNewTemplateModel(
                                            ConstantsManager.getInstance()
                                                    .getMessages()
                                                    .vmStorageDomainIsNotAccessible());

                                    return;
                                }
                            }
                        }

                        if (activeStorageDomainList.size() > 0)
                        {
                            if (getSystemTreeSelectedItem() != null
                                    && getSystemTreeSelectedItem().getType() == SystemTreeItemType.Storage)
                            {
                                StorageDomain selectStorage =
                                        (StorageDomain) getSystemTreeSelectedItem().getEntity();
                                StorageDomain s =
                                        Linq.firstOrDefault(activeStorageDomainList,
                                                new Linq.StoragePredicate(selectStorage.getId()));
                                activeStorageDomainList =
                                        new ArrayList<StorageDomain>(Arrays.asList(new StorageDomain[] { s }));

                                behavior.getModel().getStorageDomain().setItems(activeStorageDomainList);
                                behavior.getModel().getStorageDomain().setIsChangable(false);
                                behavior.getModel().getStorageDomain().setSelectedItem(s);
                            }
                            else
                            {
                                behavior.getModel().getStorageDomain().setItems(activeStorageDomainList);
                                behavior.getModel().getStorageDomain().setIsChangable(true);
                            }
                        }
                        else
                        {
                            behavior.disableNewTemplateModel(ConstantsManager.getInstance()
                                    .getMessages()
                                    .noActiveStorageDomain());
                        }

                        ArrayList<DiskModel> disks =
                                (ArrayList<DiskModel>) behavior.getModel().getDisksAllocationModel().getDisks();

                        Collections.sort(activeStorageDomainList, new NameableComparator());
                        if (disks != null) {
                            for (DiskModel diskModel : disks) {
                                diskModel.getStorageDomain().setItems(activeStorageDomainList);
                                diskModel.getQuota().setItems(behavior.getModel().getQuota().getItems());
                            }
                        }
                    }
                }),
                vm.getStoragePoolId(),
                ActionGroup.CREATE_TEMPLATE);
    }

    private void disableNewTemplateModel(String errMessage)
    {
        getModel().setIsValid(false);
        getModel().setMessage(errMessage);
        getModel().getName().setIsChangable(false);
        getModel().getDescription().setIsChangable(false);
        getModel().getDataCenterWithClustersList().setIsChangable(false);
        getModel().getComment().setIsChangable(false);
        getModel().getStorageDomain().setIsChangable(false);
        getModel().getIsTemplatePublic().setIsChangable(false);
        getModel().getDefaultCommand().setIsAvailable(false);
    }

    @Override
    public void postOsItemChanged() {
        if (vm != null) {
            getModel().getOSType().setSelectedItem(vm.getOs());
        }
    }

    @Override
    protected void baseTemplateSelectedItemChanged() {
        if (getModel().getBaseTemplate().getSelectedItem() != null && getModel().getIsSubTemplate().getEntity()) {
            // template.name for version should be the same as the base template name
            getModel().getName().setEntity(getModel().getBaseTemplate().getSelectedItem().getName());
        }
    }

    @Override
    protected void isSubTemplateEntityChanged() {
        getModel().getName().setIsChangable(!getModel().getIsSubTemplate().getEntity());
        if (!getModel().getIsSubTemplate().getEntity()) {
            getModel().getName().setEntity(""); //$NON-NLS-1$
        } else {
            // there will always be at least 'Blank' base template
            getModel().getBaseTemplate().setSelectedItem(getModel().getBaseTemplate().getItems().iterator().next());

            // copy any entered name to be the template-version name
            getModel().getTemplateVersionName().setEntity(getModel().getName().getEntity());
            getModel().getName().setEntity(getModel().getBaseTemplate().getSelectedItem().getName());
        }
    }

    @Override
    public void enableSinglePCI(boolean enabled) {
        super.enableSinglePCI(enabled);
        getModel().getIsSingleQxlEnabled().setEntity(vm.getSingleQxlPci());
    }
}
