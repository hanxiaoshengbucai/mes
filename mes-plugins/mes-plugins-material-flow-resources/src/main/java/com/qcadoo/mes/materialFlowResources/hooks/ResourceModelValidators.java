/**
 * ***************************************************************************
 * Copyright (c) 2010 Qcadoo Limited
 * Project: Qcadoo MES
 * Version: 1.4
 *
 * This file is part of Qcadoo.
 *
 * Qcadoo is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation; either version 3 of the License,
 * or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 * ***************************************************************************
 */
package com.qcadoo.mes.materialFlowResources.hooks;

import static com.qcadoo.mes.materialFlow.constants.LocationFields.TYPE;
import static com.qcadoo.mes.materialFlowResources.constants.ResourceFields.LOCATION;

import java.util.Date;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.qcadoo.mes.advancedGenealogy.constants.ProductFieldsAG;
import com.qcadoo.mes.materialFlowResources.PalletValidatorService;
import com.qcadoo.mes.materialFlowResources.constants.LocationFieldsMFR;
import com.qcadoo.mes.materialFlowResources.constants.ResourceFields;
import com.qcadoo.mes.materialFlowResources.validators.PositionValidators;
import com.qcadoo.model.api.DataDefinition;
import com.qcadoo.model.api.Entity;

@Service
public class ResourceModelValidators {

    @Autowired
    private PalletValidatorService palletValidatorService;

    @Autowired
    private PositionValidators positionValidators;

    public boolean validatesWith(final DataDefinition resourceDD, final Entity resource) {
        boolean isValid = true;

        isValid = isValid && checkIfLocationIsWarehouse(resourceDD, resource);
        isValid = isValid && checkQuantities(resourceDD, resource);
        isValid = isValid && checkPallet(resourceDD, resource);
        isValid = isValid && checkProductionAndExpirationDate(resourceDD, resource);
        isValid = isValid && validateRequiredAttributes(resourceDD, resource);
        isValid = isValid && checBatchEvidence(resourceDD, resource);

        return isValid;
    }

    private boolean checkIfLocationIsWarehouse(final DataDefinition resourceDD, final Entity resource) {
        Entity location = resource.getBelongsToField(LOCATION);

        if (Objects.nonNull(location)) {
            String type = location.getStringField(TYPE);

            if (!"02warehouse".equals(type)) {
                resource.addError(resourceDD.getField(LOCATION),
                        "materialFlowResources.validate.global.error.locationIsNotWarehouse");

                return false;
            }
        }

        return true;
    }

    private boolean checkQuantities(final DataDefinition resourceDD, final Entity resource) {
        // BigDecimal quantity = resource.getDecimalField(ResourceFields.QUANTITY);
        // BigDecimal reservedQuantity = resource.getDecimalField(ResourceFields.RESERVED_QUANTITY);
        // BigDecimal availableQuantity = resource.getDecimalField(ResourceFields.AVAILABLE_QUANTITY);
        // if (quantity == null || reservedQuantity == null || availableQuantity == null) {
        // resource.addGlobalError("materialFlow.error.correction.invalidQuantity");
        // return false;
        // }
        // if (availableQuantity.compareTo(quantity.subtract(reservedQuantity)) != 0) {
        // resource.addGlobalError("materialFlow.error.correction.invalidQuantity");
        // return false;
        // }
        return true;
    }

    private boolean checkPallet(final DataDefinition resourceDD, final Entity resource) {
        if (resource.getField(ResourceFields.VALIDATE_PALLET) == null
                || resource.getBooleanField(ResourceFields.VALIDATE_PALLET)) {
            return palletValidatorService.validatePalletForResource(resource);
        }

        return true;
    }

    private boolean checkProductionAndExpirationDate(final DataDefinition resourceDD, final Entity resource) {
        Date productionDate = resource.getDateField(ResourceFields.PRODUCTION_DATE);
        Date expirationDate = resource.getDateField(ResourceFields.EXPIRATION_DATE);

        boolean isValid = Objects.isNull(expirationDate) || Objects.isNull(productionDate)
                || productionDate.before(expirationDate);

        if (!isValid) {
            resource.addError(resourceDD.getField(ResourceFields.EXPIRATION_DATE),
                    "materialFlowResources.resource.validate.error.expirationBeforeProduction");
        }

        return isValid;
    }

    private boolean validateRequiredAttributes(final DataDefinition resourceDD, final Entity resource) {
        Entity warehouse = resource.getBelongsToField(ResourceFields.LOCATION);

        return positionValidators.validatePositionAttributes(resourceDD, resource,
                warehouse.getBooleanField(LocationFieldsMFR.REQUIRE_PRICE),
                warehouse.getBooleanField(LocationFieldsMFR.REQUIRE_BATCH),
                warehouse.getBooleanField(LocationFieldsMFR.REQUIRE_PRODUCTION_DATE),
                warehouse.getBooleanField(LocationFieldsMFR.REQUIRE_EXPIRATION_DATE));
    }

    private boolean checBatchEvidence(final DataDefinition resourceDD, final Entity resource) {
        boolean isValid = true;

        Entity product = resource.getBelongsToField(ResourceFields.PRODUCT);
        Entity batch = resource.getBelongsToField(ResourceFields.BATCH);

        if (Objects.nonNull(product)) {
            boolean batchEvidence = product.getBooleanField(ProductFieldsAG.BATCH_EVIDENCE);

            if (batchEvidence && Objects.isNull(batch)) {
                resource.addError(resourceDD.getField(ResourceFields.BATCH), "materialFlow.error.position.batch.required");

                isValid = false;
            }
        }

        return isValid;
    }

}
