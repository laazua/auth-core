<script setup lang="ts">
import { ref, reactive, computed, watch, nextTick } from 'vue';
import { ElForm, ElFormItem, ElInput } from 'element-plus';
import BaseButton from '@/components/BaseButton.vue';
import BaseInput from '@/components/BaseInput.vue';
import BaseSelect from '@/components/BaseSelect.vue';
import type { PermissionVO, PermissionCreateRequest, PermissionUpdateRequest } from '@/types/permission';

interface Props {
  visible: boolean;
  mode: 'create' | 'edit';
  initialData?: PermissionVO | null;
  moduleOptions: { label: string; value: number }[];
}

interface Emits {
  submit: [data: PermissionCreateRequest | PermissionUpdateRequest];
  close: [];
}

const props = withDefaults(defineProps<Props>(), {
  visible: false,
  mode: 'create',
  initialData: null,
  moduleOptions: () => [],
});

const emit = defineEmits<Emits>();

const formRef = ref<InstanceType<typeof ElForm>>();

const form = reactive<PermissionCreateRequest>({
  code: '',
  name: '',
  moduleId: 0,
  description: '',
});

const rules = {
  code: [
    { required: true, message: '请输入权限编码', trigger: 'blur' },
    { pattern: /^perm:[a-z0-9:_]+$/, message: '权限编码格式不正确，必须以 perm: 开头且仅包含小写字母、数字、冒号、下划线', trigger: 'blur' },
    { max: 100, message: '权限编码长度不能超过100位', trigger: 'blur' },
  ],
  name: [
    { required: true, message: '请输入权限名', trigger: 'blur' },
    { max: 50, message: '权限名长度不能超过50位', trigger: 'blur' },
  ],
  moduleId: [
    { required: true, message: '请选择所属模块', trigger: 'change' },
    { validator: validateModuleId, trigger: 'change' },
  ],
  description: [
    { max: 200, message: '描述长度不能超过200位', trigger: 'blur' },
  ],
};

function validateModuleId(_rule: any, value: number, callback: (error?: Error) => void) {
  if (!value || value === 0) {
    callback(new Error('请选择所属模块'));
  } else {
    callback();
  }
}

const isEditMode = computed(() => props.mode === 'edit');
const drawerTitle = computed(() => (isEditMode.value ? '编辑权限' : '新增权限'));

const resetForm = () => {
  form.code = '';
  form.name = '';
  form.moduleId = 0;
  form.description = '';
  formRef.value?.clearValidate();
};

const fillForm = (data: PermissionVO) => {
  form.code = data.code;
  form.name = data.name;
  form.moduleId = data.moduleId || 0;
  form.description = data.description || '';
};

const handleSubmit = () => {
  formRef.value?.validate((valid) => {
    if (valid) {
      if (isEditMode.value) {
        const { code, ...updateData } = form;
        emit('submit', updateData as PermissionUpdateRequest);
      } else {
        emit('submit', form as PermissionCreateRequest);
      }
    }
  });
};

const handleClose = () => {
  emit('close');
};

watch(
  () => props.visible,
  (newVal) => {
    if (newVal) {
      resetForm();
      if (isEditMode.value && props.initialData) {
        fillForm(props.initialData);
      }
      nextTick(() => {
        formRef.value?.clearValidate();
      });
    } else {
      resetForm();
    }
  },
  { immediate: true },
);

watch(
  () => props.initialData,
  (newVal) => {
    if (isEditMode.value && newVal && props.visible) {
      fillForm(newVal);
    }
  },
);

defineOptions({
  name: 'PermissionFormDrawer',
});
</script>

<template>
  <el-drawer
    v-model="props.visible"
    :title="drawerTitle"
    direction="rtl"
    size="480px"
    :before-close="handleClose"
    @close="emit('close')"
  >
    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-width="80px"
      class="permission-form"
    >
      <el-form-item
        v-if="!isEditMode"
        label="权限编码"
        prop="code"
      >
        <BaseInput
          v-model="form.code"
          placeholder="请输入权限编码 (如 perm:user:view)"
          maxlength="100"
        />
      </el-form-item>

      <el-form-item
        v-else
        label="权限编码"
      >
        <BaseInput
          v-model="form.code"
          placeholder="权限编码不可修改"
          readonly
          disabled
        />
      </el-form-item>

      <el-form-item label="权限名" prop="name">
        <BaseInput
          v-model="form.name"
          placeholder="请输入权限名"
          maxlength="50"
        />
      </el-form-item>

      <el-form-item label="所属模块" prop="moduleId">
        <BaseSelect
          v-model="form.moduleId"
          :options="props.moduleOptions"
          placeholder="请选择所属模块"
          style="width: 100%"
        />
      </el-form-item>

      <el-form-item label="描述" prop="description">
        <ElInput
          v-model="form.description"
          type="textarea"
          :rows="3"
          placeholder="请输入描述 (可选)"
          maxlength="200"
          style="width: 100%"
        />
      </el-form-item>
    </el-form>

    <template #footer>
      <div class="permission-form__footer">
        <BaseButton variant="default" @click="handleClose">取消</BaseButton>
        <BaseButton variant="primary" @click="handleSubmit">确定</BaseButton>
      </div>
    </template>
  </el-drawer>
</template>

<style scoped lang="scss">
.permission-form {
  padding: 8px 4px 0;
}

.permission-form__footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding-top: 16px;
  margin-top: 8px;
  border-top: 1px solid var(--color-border-light);
}

:deep(.el-drawer__body) {
  padding: 0 24px 24px;
}

:deep(.el-drawer__header) {
  padding: 16px 24px;
  border-bottom: 1px solid var(--color-border-light);
}

:deep(.el-drawer__title) {
  font-size: 16px;
  font-weight: 600;
  color: var(--color-text-primary);
}
</style>